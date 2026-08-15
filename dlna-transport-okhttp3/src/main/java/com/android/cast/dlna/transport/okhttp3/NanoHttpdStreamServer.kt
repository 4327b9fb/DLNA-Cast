package com.android.cast.dlna.transport.okhttp3

import fi.iki.elonen.NanoHTTPD
import org.jupnp.model.message.Connection
import org.jupnp.model.message.StreamRequestMessage
import org.jupnp.model.message.StreamResponseMessage
import org.jupnp.model.message.UpnpHeaders
import org.jupnp.model.message.UpnpMessage
import org.jupnp.model.message.UpnpRequest
import org.jupnp.protocol.ProtocolFactory
import org.jupnp.transport.Router
import org.jupnp.transport.spi.InitializationException
import org.jupnp.transport.spi.StreamServer
import org.jupnp.transport.spi.StreamServerConfiguration
import org.jupnp.transport.spi.UpnpStream
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.InetAddress
import java.net.URI

/**
 * StreamServer implementation based on NanoHTTPD.
 *
 * This replaces jUPnP's ServletStreamServerImpl + JettyServletContainer
 * because Jetty 9 uses MethodHandle.invoke which requires Android API 26+.
 *
 * NanoHTTPD is lightweight and compatible with all Android API levels.
 */
class NanoHttpdStreamServer(
    private val configuration: StreamServerConfiguration
) : StreamServer<StreamServerConfiguration> {

    private val logger = LoggerFactory.getLogger(NanoHttpdStreamServer::class.java)

    @Volatile
    private var httpServer: UpnpNanoHttpD? = null

    @Volatile
    private var localPort: Int = 0

    override fun getConfiguration(): StreamServerConfiguration = configuration

    @Synchronized
    @Throws(InitializationException::class)
    override fun init(bindAddress: InetAddress, router: Router) {
        try {
            val port = configuration.listenPort
            httpServer = UpnpNanoHttpD(port, router.protocolFactory)
            localPort = port
            logger.debug("Initialized NanoHTTPD StreamServer on port: {}", port)
        } catch (e: Exception) {
            throw InitializationException("Could not initialize NanoHTTPD StreamServer", e)
        }
    }

    @Synchronized
    override fun getPort(): Int = localPort

    @Synchronized
    override fun stop() {
        httpServer?.let {
            try {
                it.stop()
                logger.debug("NanoHTTPD StreamServer stopped")
            } catch (e: Exception) {
                logger.warn("Error stopping NanoHTTPD StreamServer", e)
            }
        }
        httpServer = null
    }

    override fun run() {
        httpServer?.let { server ->
            try {
                if (!server.wasStarted()) {
                    server.start()
                    // Update localPort with the actual listening port (important if configured as 0/ephemeral)
                    localPort = server.listeningPort
                    logger.debug("NanoHTTPD StreamServer started on port: {}", localPort)
                }
            } catch (e: IOException) {
                logger.error("Could not start NanoHTTPD StreamServer", e)
            }
        }
    }

    /**
     * NanoHTTPD subclass that handles UPnP requests.
     * Extends NanoHTTPD so we can access its protected methods and inner types.
     */
    private class UpnpNanoHttpD(
        port: Int,
        private val protocolFactory: ProtocolFactory
    ) : NanoHTTPD(port) {

        override fun serve(session: IHTTPSession): Response {
            val upnpStream = NanoHttpdUpnpStream(protocolFactory, session)
            upnpStream.run()

            val responseMessage = upnpStream.responseMessage
            return if (responseMessage != null) {
                createNanoResponse(responseMessage)
            } else {
                newFixedLengthResponse(
                    Response.Status.NOT_FOUND,
                    MIME_PLAINTEXT,
                    "Not Found"
                )
            }
        }

        private fun createNanoResponse(responseMessage: StreamResponseMessage): Response {
            val status = mapStatus(responseMessage.operation.statusCode)
            val contentType = getContentType(responseMessage)
            val bodyBytes = if (responseMessage.hasBody()) responseMessage.bodyBytes else null

            val response = if (bodyBytes != null && bodyBytes.isNotEmpty()) {
                newFixedLengthResponse(
                    status,
                    contentType,
                    ByteArrayInputStream(bodyBytes),
                    bodyBytes.size.toLong()
                )
            } else {
                newFixedLengthResponse(status, contentType, "")
            }

            // Add response headers
            for ((key, values) in responseMessage.headers) {
                for (value in values) {
                    response.addHeader(key, value)
                }
            }

            return response
        }

        private fun getContentType(message: StreamResponseMessage): String {
            // Try to get content type from the message headers
            val ctHeaders = message.headers["Content-Type"]
            if (!ctHeaders.isNullOrEmpty()) {
                return ctHeaders[0]
            }
            return MIME_PLAINTEXT
        }

        private fun mapStatus(statusCode: Int): Response.Status {
            return when (statusCode) {
                200 -> Response.Status.OK
                404 -> Response.Status.NOT_FOUND
                500 -> Response.Status.INTERNAL_ERROR
                501 -> Response.Status.NOT_IMPLEMENTED
                else -> Response.Status.OK
            }
        }
    }

    /**
     * UpnpStream implementation that reads from NanoHTTPD session and processes
     * the UPnP request. Only produces a StreamResponseMessage (or null);
     * the NanoHTTPD Response is created by UpnpNanoHttpD.
     */
    private class NanoHttpdUpnpStream(
        protocolFactory: ProtocolFactory,
        private val session: NanoHTTPD.IHTTPSession
    ) : UpnpStream(protocolFactory) {

        private val logger = LoggerFactory.getLogger(NanoHttpdUpnpStream::class.java)

        var responseMessage: StreamResponseMessage? = null
            private set

        override fun run() {
            try {
                val requestMessage = readRequestMessage()
                logger.trace(
                    "Processing UPnP request: {} {}",
                    requestMessage?.operation?.method,
                    requestMessage?.uri
                )

                val result = if (requestMessage != null) process(requestMessage) else null

                if (result != null) {
                    responseMessage = result
                    responseSent(result)
                } else {
                    // null means 404, handled by UpnpNanoHttpD
                    logger.trace("Protocol returned no response (404)")
                }
            } catch (e: Exception) {
                logger.warn("Exception during UPnP stream processing", e)
                responseException(e)
            }
        }

        private fun readRequestMessage(): StreamRequestMessage? {
            try {
                val method = session.method.name
                val uri = session.uri ?: return null

                logger.trace("Reading HTTP request: {} {}", method, uri)

                val upnpMethod = UpnpRequest.Method.getByHttpName(method)
                if (upnpMethod == UpnpRequest.Method.UNKNOWN) {
                    logger.warn("Unknown HTTP method: {}", method)
                    return null
                }

                val requestMessage = StreamRequestMessage(upnpMethod, URI.create(uri))

                // Connection wrapper
                requestMessage.connection = NanoHttpdConnection(session)

                // Headers
                val headers = UpnpHeaders()
                for ((key, value) in session.headers) {
                    // Use explicit String type to resolve overload ambiguity
                    headers.add(key as String, value as String)
                }
                requestMessage.headers = headers

                // Body - read POST/NOTIFY body from NanoHTTPD's body handling
                if (method.equals("POST", ignoreCase = true) || method.equals("NOTIFY", ignoreCase = true)) {
                    val bodyBytes = readBody()
                    if (bodyBytes.isNotEmpty()) {
                        if (requestMessage.isContentTypeMissingOrText) {
                            requestMessage.setBodyCharacters(bodyBytes)
                        } else {
                            requestMessage.setBody(UpnpMessage.BodyType.BYTES, bodyBytes)
                        }
                    }
                }

                return requestMessage
            } catch (e: Exception) {
                logger.warn("Could not read request message", e)
                return null
            }
        }

        private fun readBody(): ByteArray {
            // NanoHTTPD requires parsing the body before accessing it
            val files = HashMap<String, String>()
            try {
                session.parseBody(files)
            } catch (e: Exception) {
                logger.trace("Could not parse body: {}", e.message)
            }

            // Try to get the posted data from the "postData" key
            val postData = files["postData"]
            if (postData != null) {
                return postData.toByteArray(Charsets.UTF_8)
            }

            return ByteArray(0)
        }
    }

    /**
     * Connection implementation backed by NanoHTTPD session.
     */
    private class NanoHttpdConnection(
        private val session: NanoHTTPD.IHTTPSession
    ) : Connection {

        override fun isOpen(): Boolean = true

        override fun getRemoteAddress(): InetAddress {
            return try {
                val remoteAddr = session.remoteIpAddress ?: "127.0.0.1"
                InetAddress.getByName(remoteAddr)
            } catch (e: Exception) {
                InetAddress.getLoopbackAddress()
            }
        }

        override fun getLocalAddress(): InetAddress {
            return try {
                val localAddr = session.remoteHostName ?: "127.0.0.1"
                InetAddress.getByName(localAddr)
            } catch (e: Exception) {
                InetAddress.getLoopbackAddress()
            }
        }
    }
}