package com.android.cast.dlna.transport.okhttp3

import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.jupnp.model.ServerClientTokens
import org.jupnp.model.message.StreamRequestMessage
import org.jupnp.model.message.StreamResponseMessage
import org.jupnp.model.message.UpnpHeaders
import org.jupnp.model.message.UpnpMessage
import org.jupnp.model.message.UpnpRequest
import org.jupnp.model.message.UpnpResponse
import org.jupnp.model.message.header.UpnpHeader
import org.jupnp.transport.spi.AbstractStreamClient
import org.jupnp.transport.spi.StreamClient
import org.jupnp.transport.spi.StreamClientConfiguration
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

/**
 * StreamClient implementation based on OkHttp 3.12.x.
 *
 * OkHttp 3.12.x is the LTS branch supporting Android API 14+.
 * It uses Java-style API (no Kotlin extension functions).
 *
 * OkHttp supports arbitrary HTTP method names via [Request.Builder.method],
 * which is required for UPnP GENA eventing (SUBSCRIBE/UNSUBSCRIBE methods).
 */
class OkHttp3StreamClient(
    private val configuration: StreamClientConfiguration
) : AbstractStreamClient<StreamClientConfiguration, okhttp3.Call>() {

    private val logger = LoggerFactory.getLogger(StreamClient::class.java)

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(configuration.timeoutSeconds.toLong(), TimeUnit.SECONDS)
        .readTimeout(configuration.timeoutSeconds.toLong(), TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    override fun getConfiguration(): StreamClientConfiguration = configuration

    override fun createRequest(requestMessage: StreamRequestMessage): okhttp3.Call? {
        val upnpRequest = requestMessage.operation
        logger.trace("Creating HTTP request. URI: '{}' method: '{}'", upnpRequest.uri, upnpRequest.method)

        val url: String
        try {
            url = upnpRequest.uri.toString()
        } catch (e: Exception) {
            logger.debug("Cannot create request because URI '{}' is invalid", upnpRequest.uri.toString(), e)
            return null
        }

        val requestBuilder = Request.Builder().url(url)

        // OkHttp supports arbitrary HTTP method names (SUBSCRIBE, UNSUBSCRIBE, etc.)
        val httpMethod = upnpRequest.httpMethodName
        val hasBody = requestMessage.hasBody() &&
            (upnpRequest.method == UpnpRequest.Method.POST ||
             upnpRequest.method == UpnpRequest.Method.NOTIFY)

        if (hasBody) {
            val bodyBytes = if (requestMessage.bodyType == UpnpMessage.BodyType.STRING) {
                requestMessage.bodyString?.toByteArray(Charsets.UTF_8)
            } else {
                requestMessage.bodyBytes
            }
            // UPnP POST/NOTIFY always use text/xml content type
            val contentType = MediaType.parse("text/xml; charset=\"utf-8\"")
            val requestBody = RequestBody.create(contentType, bodyBytes ?: ByteArray(0))
            requestBuilder.method(httpMethod, requestBody)
        } else {
            // GET, SUBSCRIBE, UNSUBSCRIBE, etc. — no body
            requestBuilder.method(httpMethod, null)
        }

        // Add the default user agent if not already set on the message
        if (!requestMessage.headers.containsKey(UpnpHeader.Type.USER_AGENT)) {
            val tokens = ServerClientTokens(requestMessage.udaMajorVersion, requestMessage.udaMinorVersion)
            tokens.osName = "Android"
            requestBuilder.addHeader(UpnpHeader.Type.USER_AGENT.httpName, tokens.toString())
        }

        // Apply headers
        for (entry in requestMessage.headers) {
            for (value in entry.value) {
                requestBuilder.addHeader(entry.key, value)
            }
        }

        return client.newCall(requestBuilder.build())
    }

    override fun createCallable(
        requestMessage: StreamRequestMessage,
        request: okhttp3.Call
    ): Callable<StreamResponseMessage> {
        return Callable {
            logger.trace("Sending HTTP request: {}", requestMessage)
            try {
                val response = request.execute()

                val responseCode = response.code()
                val responseMsgText = response.message()

                logger.trace("Received HTTP response: {} {}", responseCode, responseMsgText)

                val responseOperation = UpnpResponse(responseCode, responseMsgText)
                val responseMsg = StreamResponseMessage(responseOperation)

                // Headers — OkHttp 3.x uses methods: headers(), name(i), value(i)
                val headers = UpnpHeaders()
                for (i in 0 until response.headers().size()) {
                    headers.add(response.headers().name(i), response.headers().value(i))
                }
                responseMsg.headers = headers

                // Body
                val bodyBytes = response.body()?.bytes()
                if (bodyBytes != null && bodyBytes.isNotEmpty()) {
                    if (responseMsg.isContentTypeMissingOrText) {
                        responseMsg.setBodyCharacters(bodyBytes)
                    } else {
                        responseMsg.setBody(UpnpMessage.BodyType.BYTES, bodyBytes)
                    }
                }

                response.close()
                responseMsg
            } catch (e: Exception) {
                logger.warn("HTTP request execution failed: {}", requestMessage, e)
                null
            }
        }
    }

    override fun abort(request: okhttp3.Call) {
        request.cancel()
    }

    override fun logExecutionException(t: Throwable): Boolean {
        return when (t) {
            is IllegalStateException -> {
                logger.trace("Illegal state: {}", t.message)
                true
            }
            else -> t.message?.contains("HTTP protocol violation") == true
        }
    }

    override fun stop() {
        logger.trace("Shutting down OkHttp3 StreamClient")
        // OkHttp 3.x: dispatcher() returns Dispatcher, executorService() returns ExecutorService
        client.dispatcher().executorService().shutdown()
        client.connectionPool().evictAll()
    }
}