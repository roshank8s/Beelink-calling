package com.beelinking.bridge.bluetooth

import java.util.UUID

object BluetoothConstants {
    /** Custom UUID for the Beelink Calling Bridge RFCOMM service. */
    val SERVICE_UUID: UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    const val SERVICE_NAME = "BeelinkCallingBridge"

    /** Audio stream uses a separate RFCOMM channel for low-latency. */
    val AUDIO_UUID: UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567891")
    const val AUDIO_SERVICE_NAME = "BeelinkAudioBridge"
}
