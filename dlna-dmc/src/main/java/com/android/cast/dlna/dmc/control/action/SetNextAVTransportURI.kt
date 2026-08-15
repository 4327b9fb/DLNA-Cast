package com.android.cast.dlna.dmc.control.action

import org.jupnp.controlpoint.ActionCallback
import org.jupnp.model.action.ActionInvocation
import org.jupnp.model.meta.Service
import org.jupnp.model.types.UnsignedIntegerFourBytes
import org.slf4j.LoggerFactory

@Suppress("LeakingThis")
abstract class SetNextAVTransportURI @JvmOverloads constructor(
    service: Service<*, *>?,
    uri: String,
    metadata: String? = null,
) : ActionCallback(ActionInvocation(service?.getAction("SetNextAVTransportURI"))) {
    companion object {
        private val log = LoggerFactory.getLogger(SetNextAVTransportURI::class.java)
    }

    init {
        log.debug("Creating SetNextAVTransportURI action for URI: $uri")
        getActionInvocation().setInput("InstanceID", UnsignedIntegerFourBytes(0))
        getActionInvocation().setInput("NextURI", uri)
        getActionInvocation().setInput("NextURIMetaData", metadata)
    }

    override fun success(invocation: ActionInvocation<*>?) {
        log.debug("Execution successful")
    }
}