package com.android.cast.dlna.transport.jetty

import com.android.cast.dlna.core.transport.TransportFactory
import org.jupnp.transport.impl.ServletStreamServerConfigurationImpl
import org.jupnp.transport.impl.ServletStreamServerImpl
import org.jupnp.transport.impl.jetty.JettyServletContainer
import org.jupnp.transport.impl.jetty.JettyStreamClientImpl
import org.jupnp.transport.impl.jetty.StreamClientConfigurationImpl
import org.jupnp.transport.spi.StreamClient
import org.jupnp.transport.spi.StreamClientConfiguration
import org.jupnp.transport.spi.StreamServer
import org.jupnp.transport.spi.StreamServerConfiguration
import org.slf4j.LoggerFactory

/**
 * Jetty transport factory — pure Jetty client + server stack.
 *
 * Provides [JettyStreamClientImpl] + [ServletStreamServerImpl] with [JettyServletContainer].
 *
 * **Android compatibility warning:**
 * Jetty 9 uses `MethodHandle.invoke`/`invokeExact` which requires Android API 26+.
 * Use [com.android.cast.dlna.transport.okhttp3.OkHttp3TransportFactory] or
 * [com.android.cast.dlna.transport.okhttp4.OkHttp4TransportFactory] for broader compatibility.
 */
class JettyTransportFactory : TransportFactory {

    private val logger = LoggerFactory.getLogger(JettyTransportFactory::class.java)

    override fun createStreamClient(configuration: StreamClientConfiguration): StreamClient<*> {
        logger.info("Creating Jetty-based StreamClient (requires API 26+ on Android)")
        val jettyConfig = StreamClientConfigurationImpl(
            configuration.requestExecutorService,
            configuration.timeoutSeconds
        )
        return JettyStreamClientImpl(jettyConfig)
    }

    override fun createStreamServer(configuration: StreamServerConfiguration): StreamServer<*> {
        logger.info("Creating Jetty-based StreamServer (requires API 26+ on Android)")
        return ServletStreamServerImpl(
            ServletStreamServerConfigurationImpl(
                JettyServletContainer.INSTANCE,
                configuration.listenPort
            )
        )
    }
}