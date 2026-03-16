package com.beelinking.bridge.ui.screens

import androidx.compose.foundation.layout.*
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
import com.beelinking.bridge.ui.theme.BluetoothBlue
import com.beelinking.bridge.ui.theme.CallGreen

@Composable
fun HomeScreen(
    connectionState: ConnectionState,
    remoteDeviceName: String,
    onNavigateToDialer: () -> Unit,
    onNavigateToMessages: () -> Unit,
    onNavigateToConnection: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Beelink Calling",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Connection status chip
        val isConnected = connectionState == ConnectionState.CONNECTED
        AssistChip(
            onClick = onNavigateToConnection,
            label = {
                Text(
                    if (isConnected) "Connected to $remoteDeviceName"
                    else "Not Connected"
                )
            },
            leadingIcon = {
                Icon(
                    if (isConnected) Icons.Default.BluetoothConnected
                    else Icons.Default.BluetoothDisabled,
                    contentDescription = null,
                    tint = if (isConnected) CallGreen else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Action cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActionCard(
                title = "Phone",
                subtitle = "Make & receive calls",
                icon = Icons.Default.Call,
                color = CallGreen,
                enabled = isConnected,
                onClick = onNavigateToDialer,
                modifier = Modifier.weight(1f)
            )
            ActionCard(
                title = "Messages",
                subtitle = "Send & receive SMS",
                icon = Icons.Default.Message,
                color = BluetoothBlue,
                enabled = isConnected,
                onClick = onNavigateToMessages,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // How it works
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "How it works",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow("1.", "Your SIM device stays in your pocket/bag")
                InfoRow("2.", "This WiFi device connects to it via Bluetooth")
                InfoRow("3.", "Make calls and send SMS from this device")
                InfoRow("4.", "Audio is streamed in real-time over Bluetooth")
            }
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = if (enabled) 0.12f else 0.05f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) color else color.copy(alpha = 0.3f),
                modifier = Modifier.size(36.dp)
            )
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (enabled) 1f else 0.4f
                    )
                )
            }
        }
    }
}

@Composable
private fun InfoRow(number: String, text: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            number,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = BluetoothBlue,
            modifier = Modifier.width(24.dp)
        )
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
