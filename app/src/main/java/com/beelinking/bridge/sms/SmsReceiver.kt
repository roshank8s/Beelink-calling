package com.beelinking.bridge.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.beelinking.bridge.service.BridgeService

/**
 * Receives incoming SMS on the SIM device and forwards them to the bridge service.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (sms in messages) {
            val sender = sms.displayOriginatingAddress ?: continue
            val body = sms.displayMessageBody ?: continue
            val timestamp = sms.timestampMillis

            BridgeService.instance?.onSmsReceived(sender, body, timestamp)
        }
    }
}
