package com.beelinking.bridge.data

import org.json.JSONObject

/**
 * Protocol messages sent between SIM device (server) and WiFi device (client) over Bluetooth RFCOMM.
 *
 * Wire format: 4-byte length prefix (big-endian) + UTF-8 JSON payload.
 */
sealed class BridgeMessage {
    abstract fun toJson(): JSONObject

    fun serialize(): ByteArray {
        val json = toJson().toString().toByteArray(Charsets.UTF_8)
        val length = json.size
        return byteArrayOf(
            (length shr 24 and 0xFF).toByte(),
            (length shr 16 and 0xFF).toByte(),
            (length shr 8 and 0xFF).toByte(),
            (length and 0xFF).toByte()
        ) + json
    }

    // --- Call messages ---

    /** WiFi device requests SIM device to place a call */
    data class DialRequest(val phoneNumber: String) : BridgeMessage() {
        override fun toJson() = JSONObject().apply {
            put("type", "dial_request")
            put("phone_number", phoneNumber)
        }
    }

    /** SIM device reports an incoming call */
    data class IncomingCall(val phoneNumber: String, val contactName: String?) : BridgeMessage() {
        override fun toJson() = JSONObject().apply {
            put("type", "incoming_call")
            put("phone_number", phoneNumber)
            put("contact_name", contactName ?: "")
        }
    }

    /** Answer an incoming/ringing call */
    data class AnswerCall(val accept: Boolean) : BridgeMessage() {
        override fun toJson() = JSONObject().apply {
            put("type", "answer_call")
            put("accept", accept)
        }
    }

    /** Call state changed */
    data class CallStateChanged(val state: CallState, val phoneNumber: String) : BridgeMessage() {
        override fun toJson() = JSONObject().apply {
            put("type", "call_state")
            put("state", state.name)
            put("phone_number", phoneNumber)
        }
    }

    /** End the current call */
    object HangUp : BridgeMessage() {
        override fun toJson() = JSONObject().apply { put("type", "hang_up") }
    }

    /** DTMF tone during call */
    data class DtmfTone(val digit: Char) : BridgeMessage() {
        override fun toJson() = JSONObject().apply {
            put("type", "dtmf")
            put("digit", digit.toString())
        }
    }

    // --- SMS messages ---

    /** WiFi device requests SIM device to send an SMS */
    data class SendSms(val phoneNumber: String, val body: String) : BridgeMessage() {
        override fun toJson() = JSONObject().apply {
            put("type", "send_sms")
            put("phone_number", phoneNumber)
            put("body", body)
        }
    }

    /** SIM device forwards a received SMS to WiFi device */
    data class ReceivedSms(
        val phoneNumber: String,
        val body: String,
        val timestamp: Long
    ) : BridgeMessage() {
        override fun toJson() = JSONObject().apply {
            put("type", "received_sms")
            put("phone_number", phoneNumber)
            put("body", body)
            put("timestamp", timestamp)
        }
    }

    /** SMS delivery report */
    data class SmsStatus(val phoneNumber: String, val delivered: Boolean) : BridgeMessage() {
        override fun toJson() = JSONObject().apply {
            put("type", "sms_status")
            put("phone_number", phoneNumber)
            put("delivered", delivered)
        }
    }

    // --- Audio streaming ---

    /** Raw PCM audio chunk for call audio forwarding */
    data class AudioData(val pcmData: ByteArray) : BridgeMessage() {
        override fun toJson() = JSONObject().apply {
            put("type", "audio_data")
            put("pcm_base64", android.util.Base64.encodeToString(pcmData, android.util.Base64.NO_WRAP))
        }

        override fun equals(other: Any?) = other is AudioData && pcmData.contentEquals(other.pcmData)
        override fun hashCode() = pcmData.contentHashCode()
    }

    // --- Connection management ---

    data class DeviceInfo(val deviceName: String, val hasSimCard: Boolean) : BridgeMessage() {
        override fun toJson() = JSONObject().apply {
            put("type", "device_info")
            put("device_name", deviceName)
            put("has_sim", hasSimCard)
        }
    }

    object Ping : BridgeMessage() {
        override fun toJson() = JSONObject().apply { put("type", "ping") }
    }

    object Pong : BridgeMessage() {
        override fun toJson() = JSONObject().apply { put("type", "pong") }
    }

    companion object {
        fun deserialize(data: ByteArray): BridgeMessage {
            val json = JSONObject(String(data, Charsets.UTF_8))
            return when (json.getString("type")) {
                "dial_request" -> DialRequest(json.getString("phone_number"))
                "incoming_call" -> IncomingCall(
                    json.getString("phone_number"),
                    json.optString("contact_name").takeIf { it.isNotEmpty() }
                )
                "answer_call" -> AnswerCall(json.getBoolean("accept"))
                "call_state" -> CallStateChanged(
                    CallState.valueOf(json.getString("state")),
                    json.getString("phone_number")
                )
                "hang_up" -> HangUp
                "dtmf" -> DtmfTone(json.getString("digit")[0])
                "send_sms" -> SendSms(json.getString("phone_number"), json.getString("body"))
                "received_sms" -> ReceivedSms(
                    json.getString("phone_number"),
                    json.getString("body"),
                    json.getLong("timestamp")
                )
                "sms_status" -> SmsStatus(json.getString("phone_number"), json.getBoolean("delivered"))
                "audio_data" -> AudioData(
                    android.util.Base64.decode(json.getString("pcm_base64"), android.util.Base64.NO_WRAP)
                )
                "device_info" -> DeviceInfo(json.getString("device_name"), json.getBoolean("has_sim"))
                "ping" -> Ping
                "pong" -> Pong
                else -> throw IllegalArgumentException("Unknown message type: ${json.getString("type")}")
            }
        }
    }
}

enum class CallState {
    IDLE, DIALING, RINGING, ACTIVE, ON_HOLD, DISCONNECTED
}
