package com.android.cast.dlna.core.transport

import com.android.cast.dlna.core.transport.client.AndroidStreamClientConfiguration
import org.jupnp.android.AndroidUpnpServiceConfiguration
import org.jupnp.model.Namespace
import org.jupnp.transport.spi.NetworkAddressFactory
import org.jupnp.transport.spi.StreamClient
import org.jupnp.transport.spi.StreamServer
import org.jupnp.transport.spi.StreamServerConfiguration
import java.util.ServiceLoader

/**
 * Android UPnP service configuration with pluggable HTTP transport.
 *
 * ## Transport Layer Selection
 *
 * [TransportFactory] is auto-discovered via [ServiceLoader] (Java SPI).
 * Simply add **one** transport module as a dependency:
 *
 * ```groovy
 * // Pick one:
 * api(project(':dlna-transport-okhttp3'))   // OkHttp 3 client + NanoHTTPD server
 * api(project(':dlna-transport-okhttp4'))   // OkHttp 4 client + NanoHTTPD server
 * api(project(':dlna-transport-jetty'))     // Jetty client  + Jetty server
 * ```
 *
 * No code changes are needed — the factory on the classpath is automatically used.
 *
 * To override auto-discovery, pass an explicit factory:
 * ```kotlin
 * AndroidUpnpServiceConfiguration(
 *     transportFactory = MyCustomTransportFactory()
 * )
 * ```
 *
 * ## Namespace
 *
 * The Namespace is set to "/" because NanoHTTPD doesn't use servlet context paths.
 * When using the Jetty server, the namespace is still "/" for consistency.
 */
open class AndroidUpnpServiceConfiguration(
    streamListenPort: Int = 0,
    multicastResponsePort: Int = 0,
    private val transportFactory: TransportFactory = loadFactory(TransportFactory::class.java)
) : AndroidUpnpServiceConfiguration(streamListenPort, multicastResponsePort) {

    override fun createNamespace(): Namespace {
        return Namespace("/")
    }

    override fun createStreamClient(): StreamClient<*> {
        val config = AndroidStreamClientConfiguration(
            syncProtocolExecutorService,
            timeoutSeconds = 10
        )
        return transportFactory.createStreamClient(config)
    }

    override fun createStreamServer(networkAddressFactory: NetworkAddressFactory): StreamServer<*> {
        val config = StreamServerConfiguration { networkAddressFactory.streamListenPort }
        return transportFactory.createStreamServer(config)
    }

    companion object {
        /**
         * Auto-discovers a factory via Java SPI ([ServiceLoader]).
         *
         * @param T factory type
         * @throws IllegalStateException if no factory is found (no transport module on classpath)
         */
        private inline fun <reified T : Any> loadFactory(type: Class<T>): T {
            val loader = ServiceLoader.load(type)
            val factory = loader.iterator().asSequence().firstOrNull()

            return factory ?: throw IllegalStateException(
                "No ${type.simpleName} found on classpath. " +
                    "Add a transport module dependency (dlna-transport-okhttp3, " +
                    "dlna-transport-okhttp4, or dlna-transport-jetty), " +
                    "or implement ${type.simpleName} and register it via META-INF/services."
            )
        }
    }
}