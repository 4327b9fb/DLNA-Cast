package com.android.cast.dlna.transport.okhttp4

import org.jupnp.transport.spi.StreamServerConfiguration

/**
 * StreamServer configuration for NanoHTTPD-based implementation.
 */
class NanoHttpdStreamServerConfiguration(
    private val listenPort: Int = 0
) : StreamServerConfiguration {

    override fun getListenPort(): Int = listenPort
}