package com.android.cast.dlna.dmc.control

import com.android.cast.dlna.core.Logger
import org.jupnp.controlpoint.SubscriptionCallback
import org.jupnp.model.gena.CancelReason
import org.jupnp.model.gena.GENASubscription
import org.jupnp.model.message.UpnpResponse
import org.jupnp.model.meta.Service
import org.jupnp.support.lastchange.LastChangeParser

/**
 *
 */
internal class CastSubscriptionCallback(
    service: Service<*, *>?,
    requestedDurationSeconds: Int = 1800, // Cling default 1800
    private val lastChangeParser: LastChangeParser,
    private val callback: SubscriptionListener,
) : SubscriptionCallback(service, requestedDurationSeconds) {

    private val log = Logger.create("SubscriptionCallback")

    override fun failed(subscription: GENASubscription<*>, responseStatus: UpnpResponse?, exception: Exception?, defaultMsg: String?) {
        log.e("${getTag(subscription)} failed:${responseStatus}, $exception, $defaultMsg")
        executeInMainThread { callback.failed(subscription.subscriptionId) }
    }

    override fun established(subscription: GENASubscription<*>) {
        log.i("${getTag(subscription)} established")
        executeInMainThread { callback.established(subscription.subscriptionId) }
    }

    override fun ended(subscription: GENASubscription<*>, reason: CancelReason?, responseStatus: UpnpResponse?) {
        log.w("${getTag(subscription)} ended: $reason, $responseStatus")
        executeInMainThread { callback.ended(subscription.subscriptionId) }
    }

    override fun eventsMissed(subscription: GENASubscription<*>, numberOfMissedEvents: Int) {
        log.w("${getTag(subscription)} eventsMissed: $numberOfMissedEvents")
    }

    override fun eventReceived(subscription: GENASubscription<*>) {
        val lastChangeEventValue = subscription.currentValues["LastChange"]?.value?.toString()
        if (lastChangeEventValue.isNullOrBlank()) return
        log.i("${getTag(subscription)} eventReceived: ${subscription.currentValues.keys}")
        try {
            val events = lastChangeParser.parse(lastChangeEventValue)?.instanceIDs?.firstOrNull()?.values
            events?.forEach { value ->
                log.i("    value: [${value.javaClass.simpleName}] $value")
                executeInMainThread { callback.onReceived(subscription.subscriptionId, value) }
            }
        } catch (e: Exception) {
            log.w("${getTag(subscription)} currentValues: ${subscription.currentValues}")
            e.printStackTrace()
        }
    }

    private fun getTag(subscription: GENASubscription<*>) = "[${subscription.service.serviceType.type}](${subscription.subscriptionId?.split("-")?.last()})"
}