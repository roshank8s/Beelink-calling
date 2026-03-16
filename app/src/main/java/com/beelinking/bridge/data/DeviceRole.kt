package com.beelinking.bridge.data

/** Each device runs the same APK but picks a role on setup. */
enum class DeviceRole {
    /** Phone with SIM card - acts as Bluetooth server, handles real calls/SMS */
    SIM_DEVICE,
    /** WiFi-only tablet/phone - acts as Bluetooth client, provides UI */
    WIFI_DEVICE
}
