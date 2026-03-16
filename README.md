# Beelink Calling - Bluetooth Calling & SMS Bridge

An Android app that bridges two devices via Bluetooth, allowing a WiFi-only device (tablet/phone) to make calls and send SMS through a SIM-equipped phone. Similar to OnePlus Cellular Sharing.

## How It Works

1. **SIM Device** (phone with SIM card) runs as a Bluetooth RFCOMM server
2. **WiFi Device** (tablet/phone without SIM) connects as a Bluetooth client
3. Calls and SMS are forwarded over the Bluetooth connection
4. Audio is streamed in real-time using PCM over RFCOMM

## Setup

1. Install the app on **both** devices
2. Pair the devices via Android Bluetooth settings
3. On the SIM device: select **"SIM Device"** role - it will start listening
4. On the WiFi device: select **"WiFi Device"** role - pick the SIM device from paired list
5. Once connected, use the dialer and messages from the WiFi device

## Architecture

```
WiFi Device (Client)                    SIM Device (Server)
+-----------------------+              +-----------------------+
|  Compose UI           |              |  BridgeService        |
|  - Dialer             |              |  - CallManager        |
|  - Call Screen        |  Bluetooth   |  - SmsManager         |
|  - Messages           |<--RFCOMM-->  |  - AudioBridge        |
|  - Home               |  (JSON+PCM) |  - BT Server          |
|                       |              |                       |
|  BridgeViewModel      |              |  CallStateReceiver    |
|  BluetoothClient      |              |  SmsReceiver          |
+-----------------------+              +-----------------------+
```

## Requirements

- Android 9.0 (API 28) or higher on both devices
- Bluetooth on both devices
- SIM card in one device

## Permissions

- Bluetooth (connect, scan, advertise)
- Phone (call, read state, call log)
- SMS (send, receive, read)
- Audio (record)
- Location (for Bluetooth scanning)
- Contacts (for caller ID)

## Building

```bash
./gradlew assembleDebug
```

Install on both devices:
```bash
adb -s <sim-device-serial> install app/build/outputs/apk/debug/app-debug.apk
adb -s <wifi-device-serial> install app/build/outputs/apk/debug/app-debug.apk
```
