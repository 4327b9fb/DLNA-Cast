package com.android.cast.dlna.transport.okhttp4

import com.android.cast.dlna.core.transport.TransportFactory
import org.jupnp.transport.spi.StreamClient
import org.jupnp.transport.spi.StreamClientConfiguration
import org.jupnp.transport.spi.StreamServer
import org.jupnp.transport.spi.StreamServerConfiguration

/**
 * OkHttp 4.x/5.x + NanoHTTPD transport factory.
 *
 * Provides a complete transport stack: OkHttp 4 StreamClient + NanoHTTPD StreamServer.
 * Compatible with Android API 21+.
 */
class OkHttp4TransportFactory : TransportFactory {

    override fun createStreamClient(configuration: StreamClientConfiguration): StreamClient<*> {
        return OkHttp4StreamClient(configuration)
    }

    override fun createStreamServer(configuration: StreamServerConfiguration): StreamServer<*> {
        return NanoHttpdStreamServer(configuration)
    }
}