package com.beelinking.bridge.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.beelinking.bridge.data.BridgeMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.io.InputStream

/**
 * Bluetooth RFCOMM client running on the WiFi device.
 * Connects to the SIM device's server and handles message I/O.
 */
@SuppressLint("MissingPermission")
class BluetoothClient(
    private val adapter: BluetoothAdapter,
    private val scope: CoroutineScope
) {
    private var socket: BluetoothSocket? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _incomingMessages = MutableSharedFlow<BridgeMessage>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<BridgeMessage> = _incomingMessages

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices

    private var connectJob: Job? = null
    private var readJob: Job? = null

    /** Get already paired devices */
    fun getPairedDevices(): List<BluetoothDevice> {
        return adapter.bondedDevices?.toList() ?: emptyList()
    }

    /** Connect to a specific SIM device */
    fun connectToDevice(device: BluetoothDevice) {
        connectJob?.cancel()
        connectJob = scope.launch(Dispatchers.IO) {
            try {
                _connectionState.value = ConnectionState.CONNECTING
                adapter.cancelDiscovery()

                socket = device.createRfcommSocketToServiceRecord(BluetoothConstants.SERVICE_UUID)
                socket?.connect()
                _connectionState.value = ConnectionState.CONNECTED

                // Send our device info
                sendMessage(BridgeMessage.DeviceInfo(
                    deviceName = adapter.name ?: "WiFi Device",
                    hasSimCard = false
                ))

                startReading()
            } catch (e: SecurityException) {
                _connectionState.value = ConnectionState.ERROR
                try { socket?.close() } catch (_: Exception) {}
                socket = null
            } catch (e: IOException) {
                _connectionState.value = ConnectionState.ERROR
                try { socket?.close() } catch (_: IOException) {}
                socket = null
            }
        }
    }

    private fun startReading() {
        readJob = scope.launch(Dispatchers.IO) {
            try {
                val inputStream = socket?.inputStream ?: return@launch
                while (isActive) {
                    val message = readMessage(inputStream) ?: break
                    _incomingMessages.emit(message)
                }
            } catch (e: IOException) {
                // Connection lost
            } finally {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    private fun readMessage(input: InputStream): BridgeMessage? {
        val header = ByteArray(4)
        var bytesRead = 0
        while (bytesRead < 4) {
            val r = input.read(header, bytesRead, 4 - bytesRead)
            if (r == -1) return null
            bytesRead += r
        }
        val length = (header[0].toInt() and 0xFF shl 24) or
                (header[1].toInt() and 0xFF shl 16) or
                (header[2].toInt() and 0xFF shl 8) or
                (header[3].toInt() and 0xFF)

        if (length <= 0 || length > 1_000_000) return null

        val payload = ByteArray(length)
        bytesRead = 0
        while (bytesRead < length) {
            val r = input.read(payload, bytesRead, length - bytesRead)
            if (r == -1) return null
            bytesRead += r
        }
        return BridgeMessage.deserialize(payload)
    }

    fun sendMessage(message: BridgeMessage) {
        scope.launch(Dispatchers.IO) {
            try {
                socket?.outputStream?.let { out ->
                    val data = message.serialize()
                    out.write(data)
                    out.flush()
                }
            } catch (e: IOException) {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTED
        readJob?.cancel()
        connectJob?.cancel()
        try { socket?.close() } catch (_: IOException) {}
        socket = null
    }
}
