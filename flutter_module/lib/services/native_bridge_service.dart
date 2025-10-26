import 'package:flutter/services.dart';
import 'package:flutter/material.dart';

class NativeBridgeService extends ChangeNotifier {
  static const MethodChannel _channel = MethodChannel('com.example.nativenonetapp/native');
  
  bool _bluetoothPermissionGranted = false;
  bool _locationPermissionGranted = false;
  bool _bluetoothEnabled = false;
  bool _locationSettingsEnabled = false;
  bool _isAdvertising = false;
  bool _isDiscovering = false;
  
  // Getters
  bool get bluetoothPermissionGranted => _bluetoothPermissionGranted;
  bool get locationPermissionGranted => _locationPermissionGranted;
  bool get bluetoothEnabled => _bluetoothEnabled;
  bool get locationSettingsEnabled => _locationSettingsEnabled;
  bool get isAdvertising => _isAdvertising;
  bool get isDiscovering => _isDiscovering;
  bool get allPermissionsGranted => _bluetoothPermissionGranted && _locationPermissionGranted && _bluetoothEnabled && _locationSettingsEnabled;
  
  NativeBridgeService() {
    _setupMethodCallHandler();
  }
  
  void _setupMethodCallHandler() {
    _channel.setMethodCallHandler((call) async {
      switch (call.method) {
        case 'onBluetoothPermissionResult':
          _bluetoothPermissionGranted = call.arguments;
          notifyListeners();
          break;
        case 'onLocationPermissionResult':
          _locationPermissionGranted = call.arguments;
          notifyListeners();
          break;
        case 'onBluetoothEnableResult':
          _bluetoothEnabled = call.arguments;
          notifyListeners();
          break;
        case 'onLocationSettingsResult':
          _locationSettingsEnabled = call.arguments;
          notifyListeners();
          break;
        case 'onAdvertisingStarted':
          _isAdvertising = call.arguments['success'] ?? false;
          notifyListeners();
          break;
        case 'onDiscoveryStarted':
          _isDiscovering = call.arguments['success'] ?? false;
          notifyListeners();
          break;
        case 'onDisconnected':
          // Handle partner disconnection
          notifyListeners();
          break;
        case 'onMessageReceived':
          // Handle incoming messages
          notifyListeners();
          break;
        case 'onAttachmentReceived':
          // Handle incoming attachments
          notifyListeners();
          break;
      }
    });
  }
  
  Future<void> requestBluetoothPermission() async {
    try {
      await _channel.invokeMethod('requestBluetoothPermission');
    } catch (e) {
      print('Error requesting Bluetooth permission: $e');
    }
  }
  
  Future<void> requestLocationPermission() async {
    try {
      await _channel.invokeMethod('requestLocationPermission');
    } catch (e) {
      print('Error requesting location permission: $e');
    }
  }
  
  Future<void> showBluetoothDialog() async {
    try {
      await _channel.invokeMethod('requestShowBluetoothDialog');
    } catch (e) {
      print('Error showing Bluetooth dialog: $e');
    }
  }
  
  Future<void> showLocationDialog() async {
    try {
      await _channel.invokeMethod('requestShowLocationDialog');
    } catch (e) {
      print('Error showing location dialog: $e');
    }
  }
  
  Future<void> startAdvertising(String displayName) async {
    try {
      await _channel.invokeMethod('startAdvertising', {'displayName': displayName});
    } catch (e) {
      print('Error starting advertising: $e');
    }
  }
  
  Future<void> startDiscovery() async {
    try {
      await _channel.invokeMethod('startDiscovery');
    } catch (e) {
      print('Error starting discovery: $e');
    }
  }
  
  Future<void> stopDiscovery() async {
    try {
      await _channel.invokeMethod('stopDiscovery');
    } catch (e) {
      print('Error stopping discovery: $e');
    }
  }
  
  Future<void> requestConnect(String endpointId) async {
    try {
      await _channel.invokeMethod('requestConnect', {'endpointId': endpointId});
    } catch (e) {
      print('Error requesting connection: $e');
    }
  }
  
  Future<void> acceptConnection(String endpointId) async {
    try {
      await _channel.invokeMethod('acceptConnection', {'endpointId': endpointId});
    } catch (e) {
      print('Error accepting connection: $e');
    }
  }
  
  Future<void> rejectConnection(String endpointId) async {
    try {
      await _channel.invokeMethod('rejectConnection', {'endpointId': endpointId});
    } catch (e) {
      print('Error rejecting connection: $e');
    }
  }
  
  Future<void> sendMessage(String message) async {
    try {
      await _channel.invokeMethod('sendMessage', {'message': message});
    } catch (e) {
      print('Error sending message: $e');
    }
  }
  
  Future<void> sendAttachment(String filePath) async {
    try {
      await _channel.invokeMethod('sendAttachment', {'filePath': filePath});
    } catch (e) {
      print('Error sending attachment: $e');
    }
  }
  
  Future<void> disconnect() async {
    try {
      await _channel.invokeMethod('disconnect');
    } catch (e) {
      print('Error disconnecting: $e');
    }
  }
}
