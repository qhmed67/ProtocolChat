import 'package:flutter/material.dart';

class ChatMessage {
  final String id;
  final String content;
  final bool isFromMe;
  final DateTime timestamp;
  final String? attachmentPath;
  final String? attachmentName;
  final bool isAttachment;

  ChatMessage({
    required this.id,
    required this.content,
    required this.isFromMe,
    required this.timestamp,
    this.attachmentPath,
    this.attachmentName,
    this.isAttachment = false,
  });
}

class DiscoveredDevice {
  final String endpointId;
  final String deviceName;
  final bool isConnected;

  DiscoveredDevice({
    required this.endpointId,
    required this.deviceName,
    this.isConnected = false,
  });
}

class ChatService extends ChangeNotifier {
  final List<ChatMessage> _messages = [];
  final List<DiscoveredDevice> _discoveredDevices = [];
  String? _connectedEndpointId;
  String? _connectedDeviceName;
  bool _isConnected = false;
  bool _isConnecting = false;
  String? _userName;
  
  // Getters
  List<ChatMessage> get messages => List.unmodifiable(_messages);
  List<DiscoveredDevice> get discoveredDevices => List.unmodifiable(_discoveredDevices);
  String? get connectedEndpointId => _connectedEndpointId;
  String? get connectedDeviceName => _connectedDeviceName;
  bool get isConnected => _isConnected;
  bool get isConnecting => _isConnecting;
  String? get userName => _userName;
  
  void setUserName(String name) {
    _userName = name;
    notifyListeners();
  }
  
  void addMessage(ChatMessage message) {
    _messages.add(message);
    notifyListeners();
  }
  
  void addDiscoveredDevice(DiscoveredDevice device) {
    // Remove existing device with same endpointId if any
    _discoveredDevices.removeWhere((d) => d.endpointId == device.endpointId);
    _discoveredDevices.add(device);
    notifyListeners();
  }
  
  void removeDiscoveredDevice(String endpointId) {
    _discoveredDevices.removeWhere((d) => d.endpointId == endpointId);
    notifyListeners();
  }
  
  void setConnecting(bool connecting) {
    _isConnecting = connecting;
    notifyListeners();
  }
  
  void setConnected(String endpointId, String deviceName) {
    _connectedEndpointId = endpointId;
    _connectedDeviceName = deviceName;
    _isConnected = true;
    _isConnecting = false;
    notifyListeners();
  }
  
  void setDisconnected() {
    _connectedEndpointId = null;
    _connectedDeviceName = null;
    _isConnected = false;
    _isConnecting = false;
    notifyListeners();
  }
  
  void clearChat() {
    _messages.clear();
    notifyListeners();
  }
  
  void clearDiscoveredDevices() {
    _discoveredDevices.clear();
    notifyListeners();
  }
  
  void cleanup() {
    _messages.clear();
    _discoveredDevices.clear();
    _connectedEndpointId = null;
    _connectedDeviceName = null;
    _isConnected = false;
    _isConnecting = false;
    notifyListeners();
  }
  
  void cleanupSession() {
    // Clear all chat data
    _messages.clear();
    _discoveredDevices.clear();
    
    // Reset connection state
    _connectedEndpointId = null;
    _connectedDeviceName = null;
    _isConnected = false;
    _isConnecting = false;
    
    // Clear user name for privacy
    _userName = null;
    
    notifyListeners();
  }
  
  void onPartnerDisconnected() {
    _connectedEndpointId = null;
    _connectedDeviceName = null;
    _isConnected = false;
    _isConnecting = false;
    notifyListeners();
  }
}
