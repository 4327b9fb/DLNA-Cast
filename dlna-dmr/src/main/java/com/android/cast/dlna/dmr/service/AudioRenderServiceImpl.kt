package com.android.cast.dlna.dmr.service

import org.jupnp.model.types.UnsignedIntegerFourBytes
import org.jupnp.model.types.UnsignedIntegerTwoBytes
import org.jupnp.support.model.Channel
import org.jupnp.support.renderingcontrol.AbstractAudioRenderingControl

class AudioRenderServiceImpl(private val audioControl: AudioControl) : AbstractAudioRenderingControl() {
    override fun setMute(instanceId: UnsignedIntegerFourBytes, channelName: String, desiredMute: Boolean) = audioControl.setMute(channelName, desiredMute)
    override fun getMute(instanceId: UnsignedIntegerFourBytes, channelName: String): Boolean = audioControl.getMute(channelName)
    override fun setVolume(instanceId: UnsignedIntegerFourBytes, channelName: String, desiredVolume: UnsignedIntegerTwoBytes) =
        audioControl.setVolume(channelName, desiredVolume)
    override fun getVolume(instanceId: UnsignedIntegerFourBytes, channelName: String): UnsignedIntegerTwoBytes = audioControl.getVolume(channelName)
    override fun getCurrentChannels(): Array<Channel> = arrayOf(Channel.Master)
    override fun getCurrentInstanceIds(): Array<UnsignedIntegerFourBytes> = arrayOf(UnsignedIntegerFourBytes(0))
}