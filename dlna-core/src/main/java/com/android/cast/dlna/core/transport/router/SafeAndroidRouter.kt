package com.android.cast.dlna.core.transport.router

import android.content.Context
import com.android.cast.dlna.core.Logger
import org.jupnp.UpnpServiceConfiguration
import org.jupnp.android.AndroidRouter
import org.jupnp.model.NetworkAddress
import org.jupnp.model.message.OutgoingDatagramMessage
import org.jupnp.model.message.StreamRequestMessage
import org.jupnp.model.message.StreamResponseMessage
import org.jupnp.protocol.ProtocolFactory
import org.jupnp.transport.RouterException
import java.net.InetAddress

/**
 * An [AndroidRouter] that gracefully handles [RouterException] during shutdown.
 *
 * The root cause of RouterException: [RouterImpl][org.jupnp.transport.RouterImpl] uses a
 * [ReentrantReadWriteLock][java.util.concurrent.locks.ReentrantReadWriteLock] with a bounded
 * timeout (15 seconds on Android). When `shutdown()` tries to acquire the **write lock**, active
 * callbacks (ActionCallback, SubscriptionCallback, Search) may still hold the **read lock** via
 * `send()`. If the write lock cannot be acquired within the timeout, a RouterException is thrown.
 *
 * This subclass catches RouterException in all public methods that acquire locks:
 * - **Read-lock methods** (`send`, `broadcast`, `getActiveStreamServers`): return null/empty
 *   instead of crashing. During shutdown we don't care about sending messages anyway.
 * - **Write-lock methods** (`enable`, `disable`, `shutdown`): log a warning instead of crashing.
 *   If the lock times out, the router is effectively disabled already.
 *
 * Usage: override [AndroidUpnpServiceImpl.createRouter] to return a [SafeAndroidRouter].
 */
class SafeAndroidRouter(
    configuration: UpnpServiceConfiguration,
    protocolFactory: ProtocolFactory,
    context: Context
) : AndroidRouter(configuration, protocolFactory, context) {

    private val logger = Logger.create("SafeAndroidRouter")

    // ── Read-lock methods: return null/empty on RouterException ──────────────

    override fun send(msg: StreamRequestMessage): StreamResponseMessage? {
        return try {
            super.send(msg)
        } catch (e: RouterException) {
            logger.w("Router unavailable, dropping stream request: ${e.message}")
            null
        }
    }

    override fun send(msg: OutgoingDatagramMessage<*>) {
        try {
            super.send(msg)
        } catch (e: RouterException) {
            logger.w("Router unavailable, dropping datagram: ${e.message}")
        }
    }

    override fun broadcast(bytes: ByteArray) {
        try {
            super.broadcast(bytes)
        } catch (e: RouterException) {
            logger.w("Router unavailable, dropping broadcast: ${e.message}")
        }
    }

    override fun getActiveStreamServers(preferredAddress: InetAddress?): MutableList<NetworkAddress> {
        return try {
            super.getActiveStreamServers(preferredAddress)
        } catch (e: RouterException) {
            logger.w("Router unavailable, returning empty stream servers: ${e.message}")
            mutableListOf()
        }
    }

    // ── Write-lock methods: log and swallow RouterException ──────────────────

    override fun enable(): Boolean {
        return try {
            super.enable()
        } catch (e: RouterException) {
            logger.w("Router enable failed (lock timeout), assuming already enabled: ${e.message}")
            false
        }
    }

    override fun disable(): Boolean {
        return try {
            super.disable()
        } catch (e: RouterException) {
            logger.w("Router disable failed (lock timeout), assuming already disabled: ${e.message}")
            false
        }
    }

    override fun shutdown() {
        try {
            super.shutdown()
        } catch (e: RouterException) {
            logger.w("Router shutdown failed (lock timeout), forcing cleanup: ${e.message}")
            // Best-effort: unregister broadcast receiver even if shutdown failed
            try { unregisterBroadcastReceiver() } catch (_: Exception) {}
        }
    }
}