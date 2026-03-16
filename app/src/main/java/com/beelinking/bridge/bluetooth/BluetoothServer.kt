package com.beelinking.bridge.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
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
 * Bluetooth RFCOMM server running on the SIM device.
 * Listens for connections from the WiFi device and handles message I/O.
 */
@SuppressLint("MissingPermission")
class BluetoothServer(
    private val adapter: BluetoothAdapter,
    private val scope: CoroutineScope
) {
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _incomingMessages = MutableSharedFlow<BridgeMessage>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<BridgeMessage> = _incomingMessages

    private var listenJob: Job? = null
    private var readJob: Job? = null

    fun startListening() {
        listenJob?.cancel()
        listenJob = scope.launch(Dispatchers.IO) {
            try {
                _connectionState.value = ConnectionState.LISTENING
                serverSocket = adapter.listenUsingRfcommWithServiceRecord(
                    BluetoothConstants.SERVICE_NAME,
                    BluetoothConstants.SERVICE_UUID
                )
                // Block until a client connects
                val socket = serverSocket?.accept() ?: return@launch
                clientSocket = socket
                _connectionState.value = ConnectionState.CONNECTED

                // Send our device info
                sendMessage(BridgeMessage.DeviceInfo(
                    deviceName = adapter.name ?: "SIM Device",
                    hasSimCard = true
                ))

                startReading(socket)
            } catch (e: IOException) {
                if (_connectionState.value != ConnectionState.DISCONNECTED) {
                    _connectionState.value = ConnectionState.ERROR
                }
            }
        }
    }

    private fun startReading(socket: BluetoothSocket) {
        readJob = scope.launch(Dispatchers.IO) {
            try {
                val inputStream = socket.inputStream
                while (isActive && socket.isConnected) {
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
                clientSocket?.outputStream?.let { out ->
                    val data = message.serialize()
                    out.write(data)
                    out.flush()
                }
            } catch (e: IOException) {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    fun stop() {
        _connectionState.value = ConnectionState.DISCONNECTED
        readJob?.cancel()
        listenJob?.cancel()
        try { clientSocket?.close() } catch (_: IOException) {}
        try { serverSocket?.close() } catch (_: IOException) {}
        clientSocket = null
        serverSocket = null
    }
}
