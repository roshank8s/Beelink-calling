package com.beelinking.bridge.ui.viewmodels

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beelinking.bridge.bluetooth.ConnectionState
import com.beelinking.bridge.data.BridgeMessage
import com.beelinking.bridge.data.CallState
import com.beelinking.bridge.data.DeviceRole
import com.beelinking.bridge.service.BridgeService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SmsConversation(
    val phoneNumber: String,
    val messages: List<SmsEntry>
)

data class SmsEntry(
    val body: String,
    val timestamp: Long,
    val isIncoming: Boolean,
    val delivered: Boolean = true
)

class BridgeViewModel(application: Application) : AndroidViewModel(application) {

    private val _selectedRole = MutableStateFlow<DeviceRole?>(null)
    val selectedRole: StateFlow<DeviceRole?> = _selectedRole

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState

    private val _currentNumber = MutableStateFlow("")
    val currentNumber: StateFlow<String> = _currentNumber

    private val _remoteDeviceName = MutableStateFlow("")
    val remoteDeviceName: StateFlow<String> = _remoteDeviceName

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices

    private val _smsConversations = MutableStateFlow<Map<String, SmsConversation>>(emptyMap())
    val smsConversations: StateFlow<Map<String, SmsConversation>> = _smsConversations

    private val _callDuration = MutableStateFlow(0)
    val callDuration: StateFlow<Int> = _callDuration

    private var service: BridgeService? = null

    fun selectRole(role: DeviceRole) {
        _selectedRole.value = role
        BridgeService.start(getApplication(), role)

        // Start polling for service availability and observing state
        viewModelScope.launch {
            // Wait for service to start
            while (BridgeService.instance == null) delay(100)
            service = BridgeService.instance

            service?.let { svc ->
                launch { svc.connectionState.collect { _connectionState.value = it } }
                launch { svc.callState.collect {
                    _callState.value = it
                    if (it == CallState.ACTIVE) startCallTimer() else _callDuration.value = 0
                } }
                launch { svc.currentNumber.collect { _currentNumber.value = it } }
                launch { svc.remoteDeviceName.collect { _remoteDeviceName.value = it } }
                launch { svc.incomingSms.collect { sms -> addSmsToConversation(sms) } }
                launch { svc.smsStatus.collect { /* Update delivery status */ } }
            }
        }
    }

    private fun startCallTimer() {
        viewModelScope.launch {
            _callDuration.value = 0
            while (_callState.value == CallState.ACTIVE) {
                delay(1000)
                _callDuration.value += 1
            }
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun loadPairedDevices() {
        try {
            val btManager = getApplication<Application>().getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return
            val adapter = btManager.adapter ?: return
            _pairedDevices.value = adapter.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            _pairedDevices.value = emptyList()
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        service?.getBluetoothClient()?.connectToDevice(device)
    }

    fun dial(phoneNumber: String) {
        service?.dialFromWifiDevice(phoneNumber)
    }

    fun answerCall() {
        service?.answerFromWifiDevice()
    }

    fun rejectCall() {
        service?.rejectFromWifiDevice()
    }

    fun hangUp() {
        service?.hangUpFromWifiDevice()
    }

    fun sendSms(phoneNumber: String, body: String) {
        service?.sendSmsFromWifiDevice(phoneNumber, body)
        addOutgoingSms(phoneNumber, body)
    }

    private fun addSmsToConversation(sms: BridgeMessage.ReceivedSms) {
        val current = _smsConversations.value.toMutableMap()
        val conversation = current[sms.phoneNumber] ?: SmsConversation(sms.phoneNumber, emptyList())
        val entry = SmsEntry(sms.body, sms.timestamp, isIncoming = true)
        current[sms.phoneNumber] = conversation.copy(messages = conversation.messages + entry)
        _smsConversations.value = current
    }

    private fun addOutgoingSms(phoneNumber: String, body: String) {
        val current = _smsConversations.value.toMutableMap()
        val conversation = current[phoneNumber] ?: SmsConversation(phoneNumber, emptyList())
        val entry = SmsEntry(body, System.currentTimeMillis(), isIncoming = false)
        current[phoneNumber] = conversation.copy(messages = conversation.messages + entry)
        _smsConversations.value = current
    }

    override fun onCleared() {
        super.onCleared()
        BridgeService.stop(getApplication())
    }
}
