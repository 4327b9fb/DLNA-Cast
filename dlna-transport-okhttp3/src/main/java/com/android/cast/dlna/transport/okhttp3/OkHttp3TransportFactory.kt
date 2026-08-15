package com.android.cast.dlna.transport.okhttp3

import com.android.cast.dlna.core.transport.TransportFactory
import org.jupnp.transport.spi.StreamClient
import org.jupnp.transport.spi.StreamClientConfiguration
import org.jupnp.transport.spi.StreamServer
import org.jupnp.transport.spi.StreamServerConfiguration

/**
 * OkHttp 3.12.x + NanoHTTPD transport factory.
 *
 * Provides a complete transport stack: OkHttp 3 StreamClient + NanoHTTPD StreamServer.
 * Compatible with all Android API levels (API 14+).
 */
class OkHttp3TransportFactory : TransportFactory {

    override fun createStreamClient(configuration: StreamClientConfiguration): StreamClient<*> {
        return OkHttp3StreamClient(configuration)
    }

    override fun createStreamServer(configuration: StreamServerConfiguration): StreamServer<*> {
        return NanoHttpdStreamServer(configuration)
    }
}