package com.android.cast.dlna.core.transport.client

import android.os.Build
import org.jupnp.model.ServerClientTokens
import org.jupnp.transport.spi.AbstractStreamClientConfiguration
import java.util.concurrent.ExecutorService

/**
 * StreamClient configuration for Android, based on OkHttp.
 *
 * This replaces jUPnP's Jetty-based [StreamClientConfigurationImpl]
 * to avoid Jetty 9's MethodHandle dependency (requires API 26+).
 * OkHttp also supports non-standard HTTP methods (SUBSCRIBE, UNSUBSCRIBE)
 * required by UPnP eventing, which HttpURLConnection does not support.
 */
class AndroidStreamClientConfiguration(
    requestExecutorService: ExecutorService
) : AbstractStreamClientConfiguration(requestExecutorService) {

    constructor(
        requestExecutorService: ExecutorService,
        timeoutSeconds: Int
    ) : this(requestExecutorService) {
        this.timeoutSeconds = timeoutSeconds
    }

    override fun getUserAgentValue(majorVersion: Int, minorVersion: Int): String {
        // UPNP VIOLATION: Synology NAS requires User-Agent to contain "Android"
        // to return DLNA protocolInfo required to stream to Samsung TV
        val tokens = ServerClientTokens(majorVersion, minorVersion)
        tokens.osName = "Android"
        tokens.osVersion = Build.VERSION.RELEASE
        return tokens.toString()
    }
}