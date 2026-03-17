package com.beelinking.bridge.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beelinking.bridge.bluetooth.ConnectionState
import com.beelinking.bridge.data.DeviceRole
import com.beelinking.bridge.ui.theme.BluetoothBlue
import com.beelinking.bridge.ui.theme.CallGreen

@SuppressLint("MissingPermission")
@Composable
fun ConnectionScreen(
    role: DeviceRole,
    connectionState: ConnectionState,
    remoteDeviceName: String,
    pairedDevices: List<BluetoothDevice>,
    onLoadDevices: () -> Unit,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onNavigateToHome: () -> Unit
) {
    LaunchedEffect(Unit) { onLoadDevices() }

    // Auto-navigate when connected
    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.CONNECTED) {
            kotlinx.coroutines.delay(1000) // Brief pause to show connected state
            onNavigateToHome()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Connection status
        ConnectionStatusBanner(connectionState, remoteDeviceName, role)

        Spacer(modifier = Modifier.height(32.dp))

        when (role) {
            DeviceRole.SIM_DEVICE -> {
                // SIM device just waits for connections
                Text(
                    text = "Waiting for WiFi device to connect...",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Make sure Bluetooth is enabled and this device is discoverable.\nOpen the app on your WiFi device and select this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))
                if (connectionState == ConnectionState.LISTENING) {
                    CircularProgressIndicator(color = BluetoothBlue)
                }
            }
            DeviceRole.WIFI_DEVICE -> {
                // WiFi device picks which SIM device to connect to
                Text(
                    text = "Select SIM Device",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Choose the paired device with SIM card",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (connectionState == ConnectionState.CONNECTING) {
                    CircularProgressIndicator(color = BluetoothBlue)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Connecting...")
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pairedDevices) { device ->
                        DeviceCard(
                            device = device,
                            enabled = connectionState != ConnectionState.CONNECTING,
                            onClick = { onConnectDevice(device) }
                        )
                    }

                    if (pairedDevices.isEmpty()) {
                        item {
                            Text(
                                text = "No paired devices found.\nPair your SIM device via Android Bluetooth settings first.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(onClick = onLoadDevices) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refresh")
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusBanner(
    state: ConnectionState,
    remoteName: String,
    role: DeviceRole
) {
    val (icon, label, color) = when (state) {
        ConnectionState.CONNECTED -> Triple(Icons.Default.BluetoothConnected, "Connected to $remoteName", CallGreen)
        ConnectionState.CONNECTING -> Triple(Icons.Default.BluetoothSearching, "Connecting...", BluetoothBlue)
        ConnectionState.LISTENING -> Triple(Icons.Default.BluetoothSearching, "Listening...", BluetoothBlue)
        ConnectionState.ERROR -> Triple(Icons.Default.BluetoothDisabled, "Connection error", MaterialTheme.colorScheme.error)
        ConnectionState.DISCONNECTED -> Triple(Icons.Default.BluetoothDisabled, "Disconnected", MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(label, fontWeight = FontWeight.Medium, color = color)
                Text(
                    text = if (role == DeviceRole.SIM_DEVICE) "Role: SIM Device" else "Role: WiFi Device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
private fun DeviceCard(
    device: BluetoothDevice,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val deviceName = remember(device) {
        try { device.name ?: "Unknown Device" } catch (_: SecurityException) { "Unknown Device" }
    }
    val deviceAddress = remember(device) {
        try { device.address ?: "Unknown" } catch (_: SecurityException) { "Unknown" }
    }

    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = BluetoothBlue
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = deviceName,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = deviceAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
