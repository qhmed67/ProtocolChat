# ProtocolChat - Offline Chat with Nearby Connections

*An app that i have made for Dr.Noha Presentation*

A native Android app with Flutter UI that enables secure offline chat using Google Nearby Connections API. No internet required - works entirely through Bluetooth and Wi-Fi P2P connections.

## Project Architecture

- **Host**: Native Android app written in Kotlin
- **UI**: Flutter module providing all user interface screens
- **Networking**: Google Nearby Connections API for offline peer-to-peer communication
- **Platform**: Android only (API 24+) with full support for Android 13, 14, and 15

## Features

✅ **Offline Chat**: Works without internet using Nearby Connections  
✅ **Permission Management**: Native Android dialogs for Bluetooth and Location permissions  
✅ **Android 13-15 Support**: Full compatibility with latest Android versions including Bluetooth fixes  
✅ **Device Discovery**: Scan and connect to nearby ProtocolChat users  
✅ **Secure Messaging**: Text messages and file attachments  
✅ **Ephemeral Storage**: Chat data is not permanently stored  
✅ **Modern UI**: Material 3 design with smooth animations  

## Prerequisites

- **Android Studio**: Arctic Fox (2020.3.1) or later
- **Flutter SDK**: 3.24.0 or later
- **Android SDK**: API 24+ (Android 7.0+)
- **Google Play Services**: Required for Nearby Connections
- **Two Android devices**: For testing peer-to-peer functionality

## Build Instructions

### 1. Clone and Setup

```bash
# Clone the repository
git clone <repository-url>
cd NativeNoNetApp

# Ensure Flutter SDK is available
flutter --version
```

### 2. Configure Flutter Module

```bash
# Navigate to Flutter module directory
cd flutter_module

# Get Flutter dependencies
flutter pub get

# Return to project root
cd ..
```

### 3. Build from Android Studio

1. **Open Project**: Open the `NativeNoNetApp` folder in Android Studio
2. **Sync Project**: Let Android Studio sync Gradle files
3. **Select Build Variant**: Choose `debug` or `release` in Build Variants panel
4. **Build APK**: Build → Build Bundle(s) / APK(s) → Build APK(s)

### 4. Alternative: Command Line Build

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

## Project Structure

```
NativeNoNetApp/
├── app/                          # Native Android app
│   ├── src/main/java/com/example/nativenonetapp/
│   │   ├── MainActivity.kt       # Flutter host activity
│   │   └── NearbyConnectionsManager.kt  # Nearby API integration
│   ├── build.gradle              # App dependencies
│   └── AndroidManifest.xml       # Permissions & configuration
├── flutter_module/              # Flutter UI module
│   ├── lib/
│   │   ├── main.dart            # Flutter app entry point
│   │   ├── screens/             # UI screens
│   │   │   ├── welcome_screen.dart
│   │   │   ├── user_name_screen.dart
│   │   │   ├── scan_connect_screen.dart
│   │   │   └── chat_screen.dart
│   │   └── services/            # Business logic
│   │       ├── native_bridge_service.dart
│   │       └── chat_service.dart
│   ├── pubspec.yaml             # Flutter dependencies
│   └── build.gradle             # Flutter module configuration
├── build.gradle                 # Project-level configuration
├── settings.gradle              # Module inclusion
└── README.md                    # This file
```

## Key Components

### Native Android (Kotlin)

- **MainActivity**: Flutter host with MethodChannel bridge and enhanced permission handling
- **NearbyConnectionsManager**: Google Nearby Connections API integration with Android 15+ compatibility
- **BluetoothCompatibilityHelper**: Utility class for handling Android 13-15 Bluetooth issues
- **Permission Handling**: Native dialogs for Bluetooth and Location settings with version-specific logic

### Flutter UI

- **Welcome Screen**: Permission requests with native dialogs
- **User Name Screen**: Display name input
- **Scan & Connect Screen**: Device discovery and connection
- **Chat Screen**: Messaging interface with file attachments

### MethodChannel API

The Flutter-Kotlin bridge exposes these methods:

```kotlin
// Permission dialogs
requestBluetoothPermission()
requestLocationPermission()
requestShowBluetoothDialog()
requestShowLocationDialog()

// Nearby Connections
startAdvertising(displayName)
startDiscovery()
stopDiscovery()
requestConnect(endpointId)
acceptConnection(endpointId)
rejectConnection(endpointId)

// Messaging
sendMessage(message)
sendAttachment(filePath)
disconnect()
```

## Testing Checklist

### Pre-Test Setup
- [ ] Install APK on two Android devices (API 24+)
- [ ] Ensure devices have Google Play Services
- [ ] Place devices within 10 meters of each other
- [ ] Disable internet/WiFi to test offline functionality

### Permission Testing
- [ ] **Bluetooth Permission**: Tap "Grant Bluetooth" → System dialog appears → Grant permission
- [ ] **Location Permission**: Tap "Grant Location" → System dialog appears → Grant permission  
- [ ] **Bluetooth Enable**: System Bluetooth enable dialog appears
- [ ] **Location Settings**: Google Play Services location accuracy dialog appears
- [ ] **Auto-navigation**: After both permissions granted, automatically proceeds to name screen

### User Flow Testing
- [ ] **Name Input**: Enter display name → Validation works → Continue button enabled
- [ ] **Device Discovery**: Scan screen shows "Scanning for devices..." status
- [ ] **Device List**: Nearby devices appear in list with device names
- [ ] **Connection Request**: Tap "Connect" → Connection request sent
- [ ] **Connection Accept**: Other device shows incoming connection → Accept/Decline options
- [ ] **Chat Navigation**: After connection, both devices navigate to chat screen

### Messaging Testing
- [ ] **Text Messages**: Send text message → Appears in chat bubbles with timestamps
- [ ] **Message Delivery**: Messages appear on both devices
- [ ] **File Attachments**: Tap attachment button → File picker opens → Select file → Send
- [ ] **Attachment Transfer**: File transfer shows progress → File appears in chat
- [ ] **Message History**: Chat history persists during session

### Disconnection Testing
- [ ] **Manual Disconnect**: Tap menu → Disconnect → Returns to scan screen
- [ ] **Partner Leaves**: Other device disconnects → Shows "partner left" message
- [ ] **Cache Cleanup**: After disconnect, chat history is cleared
- [ ] **Reconnection**: Can reconnect and start new chat session

### Error Handling Testing
- [ ] **Permission Denied**: Deny permissions → Appropriate error messages
- [ ] **Bluetooth Off**: Turn off Bluetooth → Error handling
- [ ] **Location Off**: Turn off location → Error handling
- [ ] **Connection Failed**: Connection timeout → Error message with retry option
- [ ] **File Transfer Failed**: Large file transfer fails → Error message

### Edge Cases
- [ ] **Multiple Devices**: More than 2 devices in range → All appear in list
- [ ] **Device Out of Range**: Move device away → Connection lost → Appropriate message
- [ ] **App Background**: Put app in background → Connection maintained
- [ ] **App Restart**: Kill and restart app → Clean state, no old data

## Android 13-15 Compatibility

This app has been specifically updated to address Bluetooth crashes and scanning issues on Android 13, 14, and 15:

### Android 13+ Changes
- **WiFi Permissions**: Added `NEARBY_WIFI_DEVICES` permission for P2P connections
- **Notification Permissions**: Added `POST_NOTIFICATIONS` permission
- **Enhanced Error Handling**: Better Bluetooth operation error handling

### Android 14+ Changes  
- **Foreground Service Permissions**: Added `FOREGROUND_SERVICE_DATA_SYNC` permission
- **Improved Bluetooth Stability**: Enhanced Bluetooth adapter error handling

### Android 15+ Changes
- **Bluetooth Crash Fixes**: Added specific handling for known Android 15 Bluetooth crashes
- **Foreground Service Permissions**: Added `FOREGROUND_SERVICE_NEARBY_DEVICE` permission
- **Enhanced Scanning**: Improved Bluetooth scanning with better error recovery
- **Permission Validation**: More robust permission checking before Bluetooth operations

### Known Issues Addressed
- ✅ Bluetooth enabling crashes on Android 15
- ✅ Bluetooth scanning failures on Samsung Galaxy S24/S25 with Android 15
- ✅ Permission handling issues on Android 13+
- ✅ Nearby Connections API compatibility issues

## Troubleshooting

### Common Issues

**Build Errors**:
- Ensure Flutter SDK is properly configured
- Check that all dependencies are resolved
- Verify Android SDK and build tools are up to date

**Permission Issues**:
- Check AndroidManifest.xml has all required permissions
- Verify runtime permission handling in MainActivity
- Test on different Android versions (API 24-34)

**Nearby Connections Issues**:
- Ensure Google Play Services is installed and updated
- Check that devices are within range (Bluetooth + WiFi)
- Verify location services are enabled

**Flutter Integration Issues**:
- Check MethodChannel names match between Flutter and Kotlin
- Verify Flutter module is properly included in settings.gradle
- Ensure Flutter dependencies are resolved

### Debug Tips

1. **Enable Logging**: Check Android Studio Logcat for native logs
2. **Flutter Debug**: Use `flutter logs` for Flutter debugging
3. **Network Testing**: Use `adb shell dumpsys connectivity` to check network state
4. **Permission Testing**: Use `adb shell dumpsys package` to check permission states

## Security & Privacy

- **No Internet**: App works entirely offline using P2P connections
- **Ephemeral Data**: Chat messages are not permanently stored
- **Local Only**: All data stays on device, no cloud storage
- **Secure Transfer**: Files transferred via encrypted Nearby Connections
- **Permission Minimal**: Only requests necessary Bluetooth and Location permissions

## Dependencies

### Native Android
- Google Play Services Nearby: 22.0.0
- Google Play Services Location: 21.0.1
- Flutter Embedding: 3.24.0

### Flutter
- permission_handler: ^11.0.1
- file_picker: ^6.1.1
- provider: ^6.1.1

## License

This project is a prototype implementation for demonstration purposes.

---

**Note**: This is a minimal working prototype. For production use, additional security measures, error handling, and UI polish would be required.
