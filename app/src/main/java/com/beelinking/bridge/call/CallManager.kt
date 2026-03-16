package com.beelinking.bridge.call

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import com.beelinking.bridge.data.CallState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages real phone calls on the SIM device.
 * Places calls, answers/rejects incoming calls, and tracks call state.
 */
@SuppressLint("MissingPermission")
class CallManager(private val context: Context) {

    private val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

    private val _currentCallState = MutableStateFlow(CallState.IDLE)
    val currentCallState: StateFlow<CallState> = _currentCallState

    private val _currentNumber = MutableStateFlow("")
    val currentNumber: StateFlow<String> = _currentNumber

    /** Place an outgoing call */
    fun dialNumber(phoneNumber: String) {
        _currentNumber.value = phoneNumber
        _currentCallState.value = CallState.DIALING
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /** Answer an incoming call (Android 8.0+) */
    fun answerCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            telecomManager.acceptRingingCall()
        }
        _currentCallState.value = CallState.ACTIVE
    }

    /** End the current call */
    fun endCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            telecomManager.endCall()
        }
        _currentCallState.value = CallState.DISCONNECTED
    }

    /** Called by CallStateReceiver when phone state changes */
    fun onPhoneStateChanged(state: Int, phoneNumber: String?) {
        phoneNumber?.let { _currentNumber.value = it }
        _currentCallState.value = when (state) {
            TelephonyManager.CALL_STATE_IDLE -> CallState.IDLE
            TelephonyManager.CALL_STATE_RINGING -> CallState.RINGING
            TelephonyManager.CALL_STATE_OFFHOOK -> CallState.ACTIVE
            else -> CallState.IDLE
        }
    }
}
