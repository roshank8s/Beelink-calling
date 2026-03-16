package com.beelinking.bridge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.beelinking.bridge.data.CallState
import com.beelinking.bridge.ui.theme.CallGreen
import com.beelinking.bridge.ui.theme.CallRed

@Composable
fun CallScreen(
    phoneNumber: String,
    callState: CallState,
    callDuration: Int,
    onAnswer: () -> Unit,
    onReject: () -> Unit,
    onHangUp: () -> Unit,
    onBack: () -> Unit
) {
    // Auto-navigate back when call ends
    LaunchedEffect(callState) {
        if (callState == CallState.IDLE || callState == CallState.DISCONNECTED) {
            kotlinx.coroutines.delay(1500)
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Caller info
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Avatar
            Surface(
                modifier = Modifier.size(96.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = phoneNumber,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (callState) {
                    CallState.DIALING -> "Dialing..."
                    CallState.RINGING -> "Incoming Call"
                    CallState.ACTIVE -> formatDuration(callDuration)
                    CallState.ON_HOLD -> "On Hold"
                    CallState.DISCONNECTED -> "Call Ended"
                    CallState.IDLE -> "Call Ended"
                },
                style = MaterialTheme.typography.titleMedium,
                color = when (callState) {
                    CallState.RINGING -> CallGreen
                    CallState.ACTIVE -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        // Call action buttons
        when (callState) {
            CallState.RINGING -> {
                // Incoming call: Answer / Reject
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Reject
                    FloatingActionButton(
                        onClick = onReject,
                        containerColor = CallRed,
                        contentColor = Color.White,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "Reject", modifier = Modifier.size(32.dp))
                    }

                    // Answer
                    FloatingActionButton(
                        onClick = onAnswer,
                        containerColor = CallGreen,
                        contentColor = Color.White,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Answer", modifier = Modifier.size(32.dp))
                    }
                }
            }
            CallState.DIALING, CallState.ACTIVE, CallState.ON_HOLD -> {
                // Active/dialing call: Hang up
                FloatingActionButton(
                    onClick = onHangUp,
                    containerColor = CallRed,
                    contentColor = Color.White,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "Hang up", modifier = Modifier.size(32.dp))
                }
            }
            else -> {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
