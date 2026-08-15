package com.android.cast.dlna.dmr

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.android.cast.dlna.core.Utils
import com.android.cast.dlna.core.getLogger
import com.android.cast.dlna.dmr.service.AVTransportController
import com.android.cast.dlna.dmr.service.AVTransportServiceImpl
import com.android.cast.dlna.dmr.service.AudioControl
import com.android.cast.dlna.dmr.service.AudioRenderController
import com.android.cast.dlna.dmr.service.AudioRenderServiceImpl
import com.android.cast.dlna.dmr.service.AvTransportControl
import org.jupnp.UpnpServiceConfiguration
import org.jupnp.UpnpServiceImpl
import com.android.cast.dlna.core.transport.AndroidUpnpServiceConfiguration
import com.android.cast.dlna.core.transport.router.SafeAndroidRouter
import org.jupnp.android.AndroidRouter
import org.jupnp.android.AndroidUpnpServiceImpl
import org.jupnp.protocol.ProtocolFactory
import org.jupnp.registry.Registry
import org.jupnp.transport.Router
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder
import org.jupnp.model.ValidationException
import org.jupnp.model.meta.DeviceDetails
import org.jupnp.model.meta.DeviceIdentity
import org.jupnp.model.meta.LocalDevice
import org.jupnp.model.meta.LocalService
import org.jupnp.model.meta.ManufacturerDetails
import org.jupnp.model.meta.ModelDetails
import org.jupnp.model.types.UDADeviceType
import org.jupnp.model.types.UDAServiceId
import org.jupnp.model.types.UDN
import org.jupnp.support.avtransport.lastchange.AVTransportLastChangeParser
import org.jupnp.support.avtransport.lastchange.AVTransportVariable
import org.jupnp.support.lastchange.EventedValue
import org.jupnp.support.lastchange.LastChangeAwareServiceManager
import org.jupnp.support.model.Channel
import org.jupnp.support.renderingcontrol.lastchange.ChannelVolume
import org.jupnp.support.renderingcontrol.lastchange.RenderingControlLastChangeParser
import org.jupnp.support.renderingcontrol.lastchange.RenderingControlVariable.Volume
import java.io.IOException
import java.util.UUID

open class DLNARendererService : AndroidUpnpServiceImpl() {
    companion object {
        fun startService(context: Context) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                context.applicationContext.startForegroundService(Intent(context, DLNARendererService::class.java))
//            } else {
            context.applicationContext.startService(Intent(context, DLNARendererService::class.java))
//            }
        }
    }

    private val logger = getLogger("RendererService")
    private val serviceBinder = RendererServiceBinderWrapper()
    private lateinit var avTransportControl: AvTransportControl
    private lateinit var audioControl: AudioControl
    private var localDevice: LocalDevice? = null

    override fun createConfiguration(): UpnpServiceConfiguration {
        return object : AndroidUpnpServiceConfiguration() {
            override fun getAliveIntervalMillis(): Int = 5 * 1000
        }
    }

    override fun onCreate() {
        logger.i("DLNARendererService create.")

        upnpService = object : UpnpServiceImpl(createConfiguration()) {
            override fun createRouter(protocolFactory: ProtocolFactory, registry: Registry): Router {
                return SafeAndroidRouter(getConfiguration(), protocolFactory, this@DLNARendererService)
            }

            override fun shutdown() {
                // Unregister broadcast receiver first to avoid Android leaking complaints
                (getRouter() as? AndroidRouter)?.unregisterBroadcastReceiver()
                // SafeAndroidRouter handles RouterException gracefully during the rest of shutdown
                super.shutdown(true)
            }
        }

        upnpService.startup()

        avTransportControl = AVTransportController(applicationContext)
        audioControl = AudioRenderController(applicationContext)
        try {
            localDevice = createRendererDevice(Utils.getHttpBaseUrl(applicationContext))
            upnpService.registry.addDevice(localDevice)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent): IBinder? = serviceBinder

    override fun onDestroy() {
        logger.w("DLNARendererService destroy.")
        localDevice?.also { device ->
            upnpService.registry.removeDevice(device)
        }
        (avTransportControl as? AVTransportController)?.mediaControl = null
        super.onDestroy()
    }

    @Throws(ValidationException::class, IOException::class)
    protected fun createRendererDevice(baseUrl: String): LocalDevice {
        val info = "DLNA_MediaPlayer-$baseUrl-${Build.MODEL}-${Build.MANUFACTURER}"
        val udn = try {
            UDN(UUID.nameUUIDFromBytes(info.toByteArray()))
        } catch (ex: Exception) {
            UDN(UUID.randomUUID())
        }
        logger.i("create local device: [MediaRenderer][${udn.identifierString.split("-").last()}]($baseUrl)")
        return LocalDevice(
            DeviceIdentity(udn),
            UDADeviceType("MediaRenderer", 1),
            DeviceDetails(
                "DMR (${Build.MODEL})",
                ManufacturerDetails(Build.MANUFACTURER),
                ModelDetails(Build.MODEL, "MPI MediaPlayer", "v1", baseUrl)
            ),
            emptyArray(),
            generateLocalServices()
        )
    }

    @Suppress("UNCHECKED_CAST")
    protected open fun generateLocalServices(): Array<LocalService<*>> {
        val serviceBinder = AnnotationLocalServiceBinder()
        // av transport service
        val avTransportService = serviceBinder.read(AVTransportServiceImpl::class.java) as LocalService<AVTransportServiceImpl>
        avTransportService.manager = object : LastChangeAwareServiceManager<AVTransportServiceImpl>(avTransportService, AVTransportLastChangeParser()) {
            override fun createServiceInstance(): AVTransportServiceImpl {
                return AVTransportServiceImpl(avTransportControl)
            }
        }
        // render service
        val renderingControlService = serviceBinder.read(AudioRenderServiceImpl::class.java) as LocalService<AudioRenderServiceImpl>
        renderingControlService.manager = object : LastChangeAwareServiceManager<AudioRenderServiceImpl>(renderingControlService, RenderingControlLastChangeParser()) {
            override fun createServiceInstance(): AudioRenderServiceImpl {
                return AudioRenderServiceImpl(audioControl)
            }
        }
        return arrayOf(avTransportService, renderingControlService)
    }

//    fun updateDevice() {
//        localDevice?.run {
//            upnpService.registry.addDevice(this)
//        }
//    }

    fun bindRealPlayer(control: RenderControl?) {
        (avTransportControl as? AVTransportController)?.mediaControl = control
    }

    fun notifyAvTransportLastChange(state: RenderState) {
        notifyAvTransportLastChange(AVTransportVariable.TransportState(state.toTransportState()))
    }

    private fun notifyAvTransportLastChange(event: EventedValue<*>) {
        val manager = localDevice?.findService(UDAServiceId("AVTransport"))?.manager
        (manager?.implementation as? AVTransportServiceImpl)?.lastChange?.setEventedValue(0, event)
        (manager as? LastChangeAwareServiceManager)?.fireLastChange()
    }

    fun notifyRenderControlLastChange(volume: Int) {
        val manager = localDevice?.findService(UDAServiceId("RenderingControl"))?.manager
        (manager?.implementation as? AudioRenderServiceImpl)?.lastChange?.setEventedValue(0, Volume(ChannelVolume(Channel.Master, volume)))
        (manager as? LastChangeAwareServiceManager)?.fireLastChange()
    }

    // ---- BinderWrapper
    protected inner class RendererServiceBinderWrapper : AndroidUpnpServiceImpl.Binder(), RendererServiceBinder {
        override val service: DLNARendererService
            get() = this@DLNARendererService
    }
}

interface RendererServiceBinder {
    val service: DLNARendererService
}
