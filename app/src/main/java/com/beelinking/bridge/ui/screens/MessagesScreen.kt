package com.beelinking.bridge.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beelinking.bridge.ui.theme.BluetoothBlue
import com.beelinking.bridge.ui.viewmodels.SmsConversation
import com.beelinking.bridge.ui.viewmodels.SmsEntry
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    conversations: Map<String, SmsConversation>,
    onSendSms: (phoneNumber: String, body: String) -> Unit,
    onBack: () -> Unit
) {
    var selectedConversation by remember { mutableStateOf<String?>(null) }
    var showNewMessage by remember { mutableStateOf(false) }

    if (selectedConversation != null) {
        val conv = conversations[selectedConversation]
        ConversationView(
            phoneNumber = selectedConversation!!,
            messages = conv?.messages ?: emptyList(),
            onSend = { body -> onSendSms(selectedConversation!!, body) },
            onBack = { selectedConversation = null }
        )
    } else if (showNewMessage) {
        NewMessageScreen(
            onSend = { number, body ->
                onSendSms(number, body)
                showNewMessage = false
                selectedConversation = number
            },
            onBack = { showNewMessage = false }
        )
    } else {
        // Conversation list
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        "Messages",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                FloatingActionButton(
                    onClick = { showNewMessage = true },
                    modifier = Modifier.size(40.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "New message", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }

            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Message,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No messages yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Incoming SMS will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(conversations.values.toList()) { conv ->
                        ConversationItem(
                            conversation = conv,
                            onClick = { selectedConversation = conv.phoneNumber }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: SmsConversation,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(22.dp),
                color = BluetoothBlue.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = BluetoothBlue)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    conversation.phoneNumber,
                    fontWeight = FontWeight.Medium
                )
                val lastMsg = conversation.messages.lastOrNull()
                if (lastMsg != null) {
                    Text(
                        lastMsg.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            val lastMsg = conversation.messages.lastOrNull()
            if (lastMsg != null) {
                Text(
                    formatTime(lastMsg.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConversationView(
    phoneNumber: String,
    messages: List<SmsEntry>,
    onSend: (String) -> Unit,
    onBack: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(phoneNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }

        HorizontalDivider()

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { entry ->
                MessageBubble(entry)
            }
        }

        HorizontalDivider()

        // Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledIconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSend(inputText.trim())
                        inputText = ""
                    }
                },
                enabled = inputText.isNotBlank()
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
private fun MessageBubble(entry: SmsEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (entry.isIncoming) Arrangement.Start else Arrangement.End
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (entry.isIncoming) 4.dp else 16.dp,
                bottomEnd = if (entry.isIncoming) 16.dp else 4.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (entry.isIncoming) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(entry.body, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    formatTime(entry.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NewMessageScreen(
    onSend: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var messageBody by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text("New Message", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Phone number") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = messageBody,
            onValueChange = { messageBody = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            label = { Text("Message") },
            maxLines = 6
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (phoneNumber.isNotBlank() && messageBody.isNotBlank()) {
                    onSend(phoneNumber.trim(), messageBody.trim())
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = phoneNumber.isNotBlank() && messageBody.isNotBlank()
        ) {
            Icon(Icons.Default.Send, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Send SMS")
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
