package com.beelinking.bridge.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.telephony.TelephonyManager
import com.beelinking.bridge.R
import com.beelinking.bridge.bluetooth.BluetoothClient
import com.beelinking.bridge.bluetooth.BluetoothServer
import com.beelinking.bridge.bluetooth.ConnectionState
import com.beelinking.bridge.call.AudioBridge
import com.beelinking.bridge.call.CallManager
import com.beelinking.bridge.data.BridgeMessage
import com.beelinking.bridge.data.CallState
import com.beelinking.bridge.data.DeviceRole
import com.beelinking.bridge.sms.SmsManager
import com.beelinking.bridge.ui.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Foreground service that keeps the Bluetooth bridge alive.
 * Routes messages between Bluetooth layer and Call/SMS managers.
 */
class BridgeService : Service() {

    companion object {
        const val CHANNEL_ID = "beelink_bridge_channel"
        const val NOTIFICATION_ID = 1
        const val EXTRA_ROLE = "device_role"

        @Volatile
        var instance: BridgeService? = null
            private set

        fun start(context: Context, role: DeviceRole) {
            val intent = Intent(context, BridgeService::class.java).apply {
                putExtra(EXTRA_ROLE, role.name)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BridgeService::class.java))
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var role: DeviceRole = DeviceRole.WIFI_DEVICE
    private var btAdapter: BluetoothAdapter? = null

    // SIM device components
    private var btServer: BluetoothServer? = null
    private var callManager: CallManager? = null
    private var smsManager: SmsManager? = null

    // WiFi device components
    private var btClient: BluetoothClient? = null

    // Shared
    private var audioBridge: AudioBridge? = null

    // Observable state for UI
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState

    private val _currentNumber = MutableStateFlow("")
    val currentNumber: StateFlow<String> = _currentNumber

    private val _remoteDeviceName = MutableStateFlow("")
    val remoteDeviceName: StateFlow<String> = _remoteDeviceName

    private val _incomingSms = MutableSharedFlow<BridgeMessage.ReceivedSms>(extraBufferCapacity = 16)
    val incomingSms: SharedFlow<BridgeMessage.ReceivedSms> = _incomingSms

    private val _smsStatus = MutableSharedFlow<BridgeMessage.SmsStatus>(extraBufferCapacity = 16)
    val smsStatus: SharedFlow<BridgeMessage.SmsStatus> = _smsStatus

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()

        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        btAdapter = btManager.adapter
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())

        val roleName = intent?.getStringExtra(EXTRA_ROLE) ?: DeviceRole.WIFI_DEVICE.name
        role = DeviceRole.valueOf(roleName)

        val adapter = btAdapter ?: run {
            stopSelf()
            return START_NOT_STICKY
        }

        audioBridge = AudioBridge(serviceScope)

        when (role) {
            DeviceRole.SIM_DEVICE -> setupSimDevice(adapter)
            DeviceRole.WIFI_DEVICE -> setupWifiDevice(adapter)
        }

        return START_STICKY
    }

    private fun setupSimDevice(adapter: BluetoothAdapter) {
        try {
            callManager = CallManager(this)
        } catch (e: Exception) {
            // TelecomManager unavailable - call features won't work but don't crash
        }
        smsManager = SmsManager(this)

        btServer = BluetoothServer(adapter, serviceScope).also { server ->
            server.startListening()

            // Forward connection state
            serviceScope.launch {
                server.connectionState.collect { _connectionState.value = it }
            }

            // Handle incoming messages from WiFi device
            serviceScope.launch {
                server.incomingMessages.collect { msg -> handleMessageOnSimDevice(msg) }
            }
        }
    }

    private fun setupWifiDevice(adapter: BluetoothAdapter) {
        btClient = BluetoothClient(adapter, serviceScope).also { client ->
            serviceScope.launch {
                client.connectionState.collect { _connectionState.value = it }
            }

            serviceScope.launch {
                client.incomingMessages.collect { msg -> handleMessageOnWifiDevice(msg) }
            }
        }
    }

    /** Handle messages received on the SIM device from WiFi device */
    private fun handleMessageOnSimDevice(msg: BridgeMessage) {
        when (msg) {
            is BridgeMessage.DialRequest -> {
                callManager?.dialNumber(msg.phoneNumber)
            }
            is BridgeMessage.AnswerCall -> {
                if (msg.accept) callManager?.answerCall() else callManager?.endCall()
            }
            is BridgeMessage.HangUp -> {
                callManager?.endCall()
            }
            is BridgeMessage.SendSms -> {
                smsManager?.sendSms(msg.phoneNumber, msg.body)
            }
            is BridgeMessage.AudioData -> {
                audioBridge?.playAudio(msg.pcmData)
            }
            is BridgeMessage.DeviceInfo -> {
                _remoteDeviceName.value = msg.deviceName
            }
            is BridgeMessage.Ping -> {
                btServer?.sendMessage(BridgeMessage.Pong)
            }
            else -> {} // Ignore messages not meant for SIM device
        }
    }

    /** Handle messages received on the WiFi device from SIM device */
    private fun handleMessageOnWifiDevice(msg: BridgeMessage) {
        when (msg) {
            is BridgeMessage.IncomingCall -> {
                _callState.value = CallState.RINGING
                _currentNumber.value = msg.phoneNumber
            }
            is BridgeMessage.CallStateChanged -> {
                _callState.value = msg.state
                _currentNumber.value = msg.phoneNumber

                when (msg.state) {
                    CallState.ACTIVE -> startAudioStreaming()
                    CallState.IDLE, CallState.DISCONNECTED -> stopAudioStreaming()
                    else -> {}
                }
            }
            is BridgeMessage.ReceivedSms -> {
                serviceScope.launch { _incomingSms.emit(msg) }
            }
            is BridgeMessage.SmsStatus -> {
                serviceScope.launch { _smsStatus.emit(msg) }
            }
            is BridgeMessage.AudioData -> {
                audioBridge?.playAudio(msg.pcmData)
            }
            is BridgeMessage.DeviceInfo -> {
                _remoteDeviceName.value = msg.deviceName
            }
            is BridgeMessage.Pong -> {} // Keep-alive response
            else -> {}
        }
    }

    private fun startAudioStreaming() {
        audioBridge?.startCapture()
        audioBridge?.startPlayback()

        // Stream captured audio over Bluetooth
        serviceScope.launch {
            audioBridge?.capturedAudio?.collect { pcmData ->
                val msg = BridgeMessage.AudioData(pcmData)
                when (role) {
                    DeviceRole.SIM_DEVICE -> btServer?.sendMessage(msg)
                    DeviceRole.WIFI_DEVICE -> btClient?.sendMessage(msg)
                }
            }
        }
    }

    private fun stopAudioStreaming() {
        audioBridge?.stop()
    }

    // --- Called by broadcast receivers ---

    fun onCallStateChanged(state: Int, phoneNumber: String) {
        callManager?.onPhoneStateChanged(state, phoneNumber)

        val bridgeState = when (state) {
            TelephonyManager.CALL_STATE_IDLE -> CallState.IDLE
            TelephonyManager.CALL_STATE_RINGING -> {
                // Also notify WiFi device of incoming call
                btServer?.sendMessage(BridgeMessage.IncomingCall(phoneNumber, null))
                CallState.RINGING
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                startAudioStreaming()
                CallState.ACTIVE
            }
            else -> CallState.IDLE
        }

        btServer?.sendMessage(BridgeMessage.CallStateChanged(bridgeState, phoneNumber))
    }

    fun onSmsReceived(sender: String, body: String, timestamp: Long) {
        btServer?.sendMessage(BridgeMessage.ReceivedSms(sender, body, timestamp))
    }

    // --- WiFi device actions (called from UI) ---

    fun getBluetoothClient() = btClient

    fun dialFromWifiDevice(phoneNumber: String) {
        btClient?.sendMessage(BridgeMessage.DialRequest(phoneNumber))
        _callState.value = CallState.DIALING
        _currentNumber.value = phoneNumber
    }

    fun answerFromWifiDevice() {
        btClient?.sendMessage(BridgeMessage.AnswerCall(true))
    }

    fun rejectFromWifiDevice() {
        btClient?.sendMessage(BridgeMessage.AnswerCall(false))
    }

    fun hangUpFromWifiDevice() {
        btClient?.sendMessage(BridgeMessage.HangUp)
        _callState.value = CallState.IDLE
        stopAudioStreaming()
    }

    fun sendSmsFromWifiDevice(phoneNumber: String, body: String) {
        btClient?.sendMessage(BridgeMessage.SendSms(phoneNumber, body))
    }

    // --- Notification ---

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_desc)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Beelink Calling Bridge")
            .setContentText("Bridge is active")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        instance = null
        audioBridge?.stop()
        btServer?.stop()
        btClient?.disconnect()
        serviceScope.cancel()
        super.onDestroy()
    }
}
