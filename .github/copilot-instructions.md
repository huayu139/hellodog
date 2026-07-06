# Copilot Instructions - HelloDog BLE Android App

## Project Overview

**HelloDog** is an Android Bluetooth Low Energy (BLE) application that enables scanning for nearby BLE devices and establishing communication with them. The app is built with Java/Gradle and targets Android SDK 34 (Android 14) with a minimum SDK of 21 (Android 5.0).

## Build, Test, and Lint Commands

### Building the App

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean build
```

### Testing

This project currently has no unit tests configured. To run tests when added:

```bash
# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests com.example.hellodog.ClassName
```

### Linting

There is no explicit linting configured in the project. Android Lint runs automatically with builds:

```bash
# Run Android Lint explicitly
./gradlew lint
```

## Architecture Overview

### Activity Flow

1. **MainActivity** → Entry point
   - Checks if device supports Bluetooth
   - Monitors Bluetooth on/off state via BroadcastReceiver
   - Transitions to BleScanActivity when "Scan BLE" button is clicked

2. **BleScanActivity** → BLE Device Discovery
   - Uses RxAndroidBle3 to scan for nearby BLE devices
   - Displays discovered devices in a ListView
   - Scans for 10 seconds then auto-stops
   - Passes selected device info to BleConnectActivity

3. **BleConnectActivity** → Device Communication
   - Establishes connection to selected BLE device
   - Subscribes to NOTIFY characteristic to receive data (UUID: 0000ffe2-...)
   - Sends hexadecimal commands via WRITE characteristic (UUID: 0000ffe1-...)
   - Displays all communication in a TextView with timestamps

### Key Dependencies

| Dependency | Version | Purpose |
|-----------|---------|---------|
| RxAndroidBle3 | 1.19.0 | BLE operations with reactive programming |
| RxJava3 | 3.1.8 | Core reactive library |
| RxAndroid | 3.0.2 | Android-specific RxJava bindings |
| EasyPermissions | 3.0.0 | Runtime permission handling |
| AndroidX AppCompat | 1.7.0 | Backward compatibility |

## Code Conventions

### Reactive Programming Pattern (RxJava)

All asynchronous operations use RxJava/RxAndroidBle patterns:

- **Subscribe pattern**: `.subscribe(onNext, onError)` with proper disposal
- **Lifecycle management**: Use `CompositeDisposable` to collect subscriptions and clear them in `onDestroy()`
- **Main thread updates**: Use `.observeOn(AndroidSchedulers.mainThread())` for UI updates
- **Error handling**: Check exception types (BleDisconnectedException, BleGattException) for specific error messages

Example:
```java
Disposable disposable = device.establishConnection(false)
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe(
        connection -> { /* success */ },
        throwable -> { /* error */ }
    );
compositeDisposable.add(disposable);
```

### Hex Data Encoding

BLE communication uses hexadecimal format for all commands and responses:

- **Display format**: Uppercase with spaces between bytes (e.g., "01 A2 FF")
- **Input/Output**: Handles space-delimited and space-free formats
- **Conversion**: `bytesToHex()` and `hexStringToBytes()` helper methods in BleConnectActivity
- **Validation**: Ensures even-length hex strings before conversion

### Permission Handling

Uses EasyPermissions library for runtime permissions:

- Required permissions: BLUETOOTH, BLUETOOTH_ADMIN, BLUETOOTH_SCAN, BLUETOOTH_CONNECT, ACCESS_FINE_LOCATION
- Permission checks happen before scanning/connecting operations
- All Activities implement `EasyPermissions.PermissionCallbacks`

### UI Patterns

- Update UI on main thread using `runOnUiThread()`
- Use Toast for brief user feedback
- Log messages use prefixed emoji and timestamps: `[HH:mm:ss.SSS] 📥 Message`
- Button states change to reflect connection status (disabled/enabled)

### BLE Characteristics (Default UUIDs)

These are placeholder UUIDs configured for demo purposes. Update based on actual device specs:

- **Service UUID**: `0000ffe0-0000-1000-8000-00805f9b34fb`
- **Write Characteristic**: `0000ffe1-0000-1000-8000-00805f9b34fb`
- **Notify Characteristic**: `0000ffe2-0000-1000-8000-00805f9b34fb`

Update these in `BleConnectActivity` if connecting to a different device.

### Resource Files

- **Layouts**: `app/src/main/res/layout/` - Contains XML files for each Activity
- **Strings**: `app/src/main/res/values/strings.xml` - App localization
- **Manifest**: `app/src/main/AndroidManifest.xml` - Activities, permissions, intent filters

## Common Development Tasks

### Adding New BLE Commands

1. Add hex command input to `BleConnectActivity`
2. Convert hex string using `hexStringToBytes()`
3. Write to WRITE_CHARACTERISTIC_UUID using `rxBleConnection.writeCharacteristic()`
4. Subscribe to result on AndroidSchedulers.mainThread()
5. Display response via `appendLog()`

### Handling New Device

1. Update SERVICE_UUID, WRITE_CHARACTERISTIC_UUID, NOTIFY_CHARACTERISTIC_UUID in BleConnectActivity
2. Verify BLE device advertises these UUIDs during scan
3. Test connection and data flow

### Debugging BLE Issues

- Check `TAG` logs in logcat for detailed error messages
- Use `getErrorMessage()` to decode BleGattException status codes
- Verify device permissions are granted (check via Settings > Apps)
- Ensure Bluetooth is enabled on device
