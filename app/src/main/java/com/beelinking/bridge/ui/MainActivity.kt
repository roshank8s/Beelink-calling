package com.beelinking.bridge.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.beelinking.bridge.data.CallState
import com.beelinking.bridge.data.DeviceRole
import com.beelinking.bridge.ui.screens.*
import com.beelinking.bridge.ui.theme.BeelinkTheme
import com.beelinking.bridge.ui.viewmodels.BridgeViewModel

class MainActivity : ComponentActivity() {

    private val requiredPermissions = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        add(Manifest.permission.CALL_PHONE)
        add(Manifest.permission.READ_PHONE_STATE)
        add(Manifest.permission.READ_CALL_LOG)
        add(Manifest.permission.SEND_SMS)
        add(Manifest.permission.RECEIVE_SMS)
        add(Manifest.permission.READ_SMS)
        add(Manifest.permission.READ_CONTACTS)
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Permissions granted or denied - UI will handle state */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionsIfNeeded()

        setContent {
            BeelinkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BeelinkNavigation()
                }
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        val needed = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }
}

@Composable
fun BeelinkNavigation() {
    val navController = rememberNavController()
    val viewModel: BridgeViewModel = viewModel()

    val selectedRole by viewModel.selectedRole.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val callState by viewModel.callState.collectAsState()
    val currentNumber by viewModel.currentNumber.collectAsState()
    val remoteDeviceName by viewModel.remoteDeviceName.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val conversations by viewModel.smsConversations.collectAsState()
    val callDuration by viewModel.callDuration.collectAsState()

    // Navigate to call screen when there's an incoming call
    LaunchedEffect(callState) {
        if (callState == CallState.RINGING || callState == CallState.ACTIVE) {
            navController.navigate("call") {
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = "role_selection") {

        composable("role_selection") {
            RoleSelectionScreen(
                onRoleSelected = { role ->
                    viewModel.selectRole(role)
                    navController.navigate("connection") {
                        popUpTo("role_selection") { inclusive = true }
                    }
                }
            )
        }

        composable("connection") {
            ConnectionScreen(
                role = selectedRole ?: DeviceRole.WIFI_DEVICE,
                connectionState = connectionState,
                remoteDeviceName = remoteDeviceName,
                pairedDevices = pairedDevices,
                onLoadDevices = { viewModel.loadPairedDevices() },
                onConnectDevice = { viewModel.connectToDevice(it) },
                onNavigateToHome = {
                    navController.navigate("home") {
                        popUpTo("connection") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                connectionState = connectionState,
                remoteDeviceName = remoteDeviceName,
                onNavigateToDialer = { navController.navigate("dialer") },
                onNavigateToMessages = { navController.navigate("messages") },
                onNavigateToConnection = { navController.navigate("connection") }
            )
        }

        composable("dialer") {
            DialerScreen(
                onDial = { number ->
                    viewModel.dial(number)
                    navController.navigate("call")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("call") {
            CallScreen(
                phoneNumber = currentNumber,
                callState = callState,
                callDuration = callDuration,
                onAnswer = { viewModel.answerCall() },
                onReject = { viewModel.rejectCall() },
                onHangUp = { viewModel.hangUp() },
                onBack = { navController.popBackStack() }
            )
        }

        composable("messages") {
            MessagesScreen(
                conversations = conversations,
                onSendSms = { number, body -> viewModel.sendSms(number, body) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
