package com.android.cast.dlna.dmr.service

import android.content.Context
import android.content.Intent
import com.android.cast.dlna.core.Logger
import com.android.cast.dlna.dmr.CastAction
import org.jupnp.model.types.ErrorCode.INVALID_ARGS
import org.jupnp.model.types.UnsignedIntegerTwoBytes
import org.jupnp.support.avtransport.AVTransportException
import org.jupnp.support.model.DeviceCapabilities
import org.jupnp.support.model.MediaInfo
import org.jupnp.support.model.PositionInfo
import org.jupnp.support.model.TransportAction
import org.jupnp.support.model.TransportInfo
import org.jupnp.support.model.TransportSettings
import java.net.URI

const val actionSetAvTransport = "com.dlna.action.SetAvTransport"
const val keyExtraCastAction = "extra.castAction"

interface RendererControl

// -------------------------------------------------------------------------------------------
// - AvTransport
// -------------------------------------------------------------------------------------------
interface AvTransportControl : RendererControl {
    val logger: Logger
    val applicationContext: Context
    fun setAVTransportURI(currentURI: String, currentURIMetaData: String?) {
        logger.i("setAVTransportURI: currentURI=$currentURI")
        currentURIMetaData?.let { logger.i("setAVTransportURI: currentURIMetaData=$it") }
        try {
            URI(currentURI)
        } catch (ex: Exception) {
            throw AVTransportException(INVALID_ARGS, "CurrentURI can not be null or malformed")
        }

        startCastActivity {
            this.currentURI = currentURI
            this.currentURIMetaData = currentURIMetaData
        }
    }

    fun setNextAVTransportURI(nextURI: String, nextURIMetaData: String?) {
        logger.i("setNextAVTransportURI: nextURI=$nextURI")
        nextURIMetaData?.let { logger.i("setNextAVTransportURI: nextURIMetaData=$it") }

        startCastActivity {
            this.nextURI = nextURI
            this.nextURIMetaData = nextURIMetaData
        }
    }

    private fun startCastActivity(content: CastAction.() -> Unit) {
        applicationContext.startActivity(Intent(actionSetAvTransport).apply {
            val castAction = CastAction()
            content(castAction)
            this.putExtra(keyExtraCastAction, castAction)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // start from service content,should add 'FLAG_ACTIVITY_NEW_TASK' flag.
        })
    }

    fun setPlayMode(newPlayMode: String?) {
        logger.i("setPlayMode: newPlayMode=$newPlayMode")
    }

    fun play(speed: String?) {
        logger.i("play: speed=$speed")
    }

    fun pause() {
        logger.i("pause")
    }

    fun seek(unit: String?, target: String?) {
        logger.i("seek: unit=$unit, target=$target")
    }

    fun previous() {
        logger.i("previous")
    }

    fun next() {
        logger.i("next")
    }

    fun stop() {
        logger.i("stop")
//        startCastActivity {
//            this.stop = "stop"
//        }
    }

    val currentTransportActions: Array<TransportAction>
    val deviceCapabilities: DeviceCapabilities
    val mediaInfo: MediaInfo
    val positionInfo: PositionInfo
    val transportInfo: TransportInfo
    val transportSettings: TransportSettings
}

// -------------------------------------------------------------------------------------------
// - Audio
// -------------------------------------------------------------------------------------------
interface AudioControl : RendererControl {
    val logger: Logger
    fun setMute(channelName: String, desiredMute: Boolean) {
        logger.i("setMute: $desiredMute")
    }

    fun getMute(channelName: String): Boolean
    fun setVolume(channelName: String, desiredVolume: UnsignedIntegerTwoBytes) {
        logger.i("setVolume: ${desiredVolume.value}")
    }

    fun getVolume(channelName: String): UnsignedIntegerTwoBytes
}