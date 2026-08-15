package com.android.cast.dlna.core.transport

import org.jupnp.transport.spi.StreamClient
import org.jupnp.transport.spi.StreamClientConfiguration
import org.jupnp.transport.spi.StreamServer
import org.jupnp.transport.spi.StreamServerConfiguration

/**
 * Factory interface for creating both [StreamClient] and [StreamServer] instances.
 *
 * This follows jUPnP's [org.jupnp.transport.TransportConfiguration] pattern,
 * pairing client and server implementations together as a complete transport stack.
 *
 * Implementations are provided by transport modules:
 *
 * - `dlna-transport-okhttp3` — OkHttp 3 client + NanoHTTPD server
 * - `dlna-transport-okhttp4` — OkHttp 4 client + NanoHTTPD server
 * - `dlna-transport-jetty`   — Jetty client + Jetty server
 *
 * Auto-discovered via [java.util.ServiceLoader]. Add one transport module as a dependency
 * and the factory on the classpath is automatically used.
 */
interface TransportFactory {

    /**
     * Creates a new [StreamClient] instance with the given configuration.
     */
    fun createStreamClient(configuration: StreamClientConfiguration): StreamClient<*>

    /**
     * Creates a new [StreamServer] instance with the given configuration.
     */
    fun createStreamServer(configuration: StreamServerConfiguration): StreamServer<*>
}