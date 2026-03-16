package com.beelinking.bridge.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import com.beelinking.bridge.ui.theme.CallGreen
import com.beelinking.bridge.ui.theme.CallRed

@Composable
fun DialerScreen(
    onDial: (String) -> Unit,
    onBack: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Phone",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Number display
        Text(
            text = phoneNumber.ifEmpty { "Enter number" },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = if (phoneNumber.isNotEmpty()) FontWeight.Normal else FontWeight.Light,
            color = if (phoneNumber.isNotEmpty()) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Dial pad
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val keys = listOf(
                listOf("1" to "", "2" to "ABC", "3" to "DEF"),
                listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
                listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
                listOf("*" to "", "0" to "+", "#" to "")
            )

            keys.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    row.forEach { (digit, letters) ->
                        DialKey(
                            digit = digit,
                            letters = letters,
                            onClick = { phoneNumber += digit },
                            onLongClick = {
                                if (digit == "0") phoneNumber += "+"
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Empty space for balance
            Spacer(modifier = Modifier.size(56.dp))

            // Call button
            FloatingActionButton(
                onClick = { if (phoneNumber.isNotEmpty()) onDial(phoneNumber) },
                containerColor = CallGreen,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Default.Call, contentDescription = "Call", modifier = Modifier.size(28.dp))
            }

            // Backspace
            IconButton(
                onClick = { if (phoneNumber.isNotEmpty()) phoneNumber = phoneNumber.dropLast(1) },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Backspace, contentDescription = "Delete")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DialKey(
    digit: String,
    letters: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = digit,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium
            )
            if (letters.isNotEmpty()) {
                Text(
                    text = letters,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
