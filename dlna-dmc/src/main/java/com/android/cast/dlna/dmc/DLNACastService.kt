package com.android.cast.dlna.dmc

import android.content.Intent
import com.android.cast.dlna.core.Logger
import org.jupnp.UpnpServiceConfiguration
import org.jupnp.UpnpServiceImpl
import com.android.cast.dlna.core.transport.AndroidUpnpServiceConfiguration
import com.android.cast.dlna.core.transport.router.SafeAndroidRouter
import org.jupnp.android.AndroidRouter
import org.jupnp.android.AndroidUpnpServiceImpl
import org.jupnp.model.types.ServiceType
import org.jupnp.protocol.ProtocolFactory
import org.jupnp.registry.Registry
import org.jupnp.transport.Router

class DLNACastService : AndroidUpnpServiceImpl() {
    private val logger = Logger.create("CastService")

    override fun onCreate() {
        logger.i("DLNACastService onCreate")

        upnpService = object : UpnpServiceImpl(createConfiguration()) {
            override fun createRouter(protocolFactory: ProtocolFactory, registry: Registry): Router {
                return SafeAndroidRouter(getConfiguration(), protocolFactory, this@DLNACastService)
            }

            override fun shutdown() {
                // Unregister broadcast receiver first to avoid Android leaking complaints
                (getRouter() as? AndroidRouter)?.unregisterBroadcastReceiver()
                // SafeAndroidRouter handles RouterException gracefully during the rest of shutdown
                super.shutdown(true)
            }
        }

        upnpService.startup()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        logger.i("DLNACastService onStartCommand: $flags, $startId, $intent")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        logger.w("DLNACastService onDestroy")
        super.onDestroy()
    }

    override fun createConfiguration(): UpnpServiceConfiguration = object : AndroidUpnpServiceConfiguration() {
        override fun getExclusiveServiceTypes(): Array<ServiceType> = arrayOf(
            DLNACastManager.SERVICE_TYPE_AV_TRANSPORT,
            DLNACastManager.SERVICE_TYPE_RENDERING_CONTROL,
            DLNACastManager.SERVICE_TYPE_CONTENT_DIRECTORY,
            DLNACastManager.SERVICE_CONNECTION_MANAGER
        )
    }
}