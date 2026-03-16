package com.beelinking.bridge.sms

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager as AndroidSmsManager

/**
 * Sends real SMS messages on the SIM device.
 */
@SuppressLint("MissingPermission")
class SmsManager(private val context: Context) {

    fun sendSms(phoneNumber: String, body: String) {
        val smsManager = context.getSystemService(AndroidSmsManager::class.java)

        val sentIntent = PendingIntent.getBroadcast(
            context, 0,
            Intent("com.beelinking.bridge.SMS_SENT").apply {
                putExtra("phone_number", phoneNumber)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Handle long messages by splitting into parts
        val parts = smsManager.divideMessage(body)
        if (parts.size == 1) {
            smsManager.sendTextMessage(phoneNumber, null, body, sentIntent, null)
        } else {
            val sentIntents = ArrayList<PendingIntent>(parts.size).apply {
                repeat(parts.size) { add(sentIntent) }
            }
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, null)
        }
    }
}
