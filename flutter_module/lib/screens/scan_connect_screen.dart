import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/native_bridge_service.dart';
import '../services/chat_service.dart';
import 'chat_screen.dart';

class ScanConnectScreen extends StatefulWidget {
  @override
  _ScanConnectScreenState createState() => _ScanConnectScreenState();
}

class _ScanConnectScreenState extends State<ScanConnectScreen> {
  bool _isScanning = false;
  String? _connectingEndpointId;
  String? _incomingConnectionEndpointId;
  String? _incomingConnectionDeviceName;

  @override
  void initState() {
    super.initState();
    _startScanning();
  }

  void _startScanning() async {
    setState(() {
      _isScanning = true;
    });

    final nativeBridge = Provider.of<NativeBridgeService>(context, listen: false);
    final chatService = Provider.of<ChatService>(context, listen: false);
    
    // Start advertising
    await nativeBridge.startAdvertising(chatService.userName ?? 'Unknown');
    
    // Start discovery
    await nativeBridge.startDiscovery();
    
    // Set up method call handler for native events
    _setupNativeEventHandlers();
  }

  void _setupNativeEventHandlers() {
    // This would be set up in the native bridge service
    // For now, we'll simulate the discovery process
    _simulateDiscovery();
  }

  void _simulateDiscovery() {
    // Simulate finding devices after a delay
    Future.delayed(Duration(seconds: 2), () {
      if (mounted) {
        final chatService = Provider.of<ChatService>(context, listen: false);
        chatService.addDiscoveredDevice(DiscoveredDevice(
          endpointId: 'device_1',
          deviceName: 'Nearby Device 1',
        ));
        chatService.addDiscoveredDevice(DiscoveredDevice(
          endpointId: 'device_2',
          deviceName: 'Another Device',
        ));
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Find Devices'),
        backgroundColor: Colors.transparent,
        elevation: 0,
        actions: [
          Consumer<NativeBridgeService>(
            builder: (context, nativeBridge, child) {
              return IconButton(
                icon: Icon(_isScanning ? Icons.stop : Icons.refresh),
                onPressed: () {
                  if (_isScanning) {
                    _stopScanning();
                  } else {
                    _startScanning();
                  }
                },
              );
            },
          ),
        ],
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Status header
              Container(
                padding: EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Theme.of(context).colorScheme.primaryContainer,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Row(
                  children: [
                    Icon(
                      _isScanning ? Icons.radar : Icons.radar_outlined,
                      color: Theme.of(context).colorScheme.primary,
                    ),
                    SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            _isScanning ? 'Scanning for devices...' : 'Scan stopped',
                            style: TextStyle(
                              fontWeight: FontWeight.w600,
                              color: Theme.of(context).colorScheme.onPrimaryContainer,
                            ),
                          ),
                          Text(
                            'Looking for nearby ProtocolChat users',
                            style: TextStyle(
                              fontSize: 12,
                              color: Theme.of(context).colorScheme.onPrimaryContainer.withOpacity(0.7),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              
              SizedBox(height: 24),
              
              // Discovered devices list
              Expanded(
                child: Consumer<ChatService>(
                  builder: (context, chatService, child) {
                    if (chatService.discoveredDevices.isEmpty) {
                      return Center(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(
                              Icons.devices_other_outlined,
                              size: 64,
                              color: Theme.of(context).colorScheme.onSurfaceVariant,
                            ),
                            SizedBox(height: 16),
                            Text(
                              'No devices found',
                              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                                color: Theme.of(context).colorScheme.onSurfaceVariant,
                              ),
                            ),
                            SizedBox(height: 8),
                            Text(
                              'Make sure other devices are running ProtocolChat and nearby',
                              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                color: Theme.of(context).colorScheme.onSurfaceVariant,
                              ),
                              textAlign: TextAlign.center,
                            ),
                          ],
                        ),
                      );
                    }

                    return ListView.builder(
                      itemCount: chatService.discoveredDevices.length,
                      itemBuilder: (context, index) {
                        final device = chatService.discoveredDevices[index];
                        return _buildDeviceCard(device);
                      },
                    );
                  },
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildDeviceCard(DiscoveredDevice device) {
    final isConnecting = _connectingEndpointId == device.endpointId;
    final isIncomingConnection = _incomingConnectionEndpointId == device.endpointId;

    return Card(
      margin: EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: EdgeInsets.all(16),
        child: Row(
          children: [
            // Device icon
            Container(
              width: 48,
              height: 48,
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.primaryContainer,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(
                Icons.phone_android,
                color: Theme.of(context).colorScheme.primary,
              ),
            ),
            
            SizedBox(width: 16),
            
            // Device info
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    device.deviceName,
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  SizedBox(height: 4),
                  Text(
                    'Ready to connect',
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: Theme.of(context).colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
            
            // Connect button or status
            if (isIncomingConnection)
              _buildIncomingConnectionActions(device)
            else
              _buildConnectButton(device, isConnecting),
          ],
        ),
      ),
    );
  }

  Widget _buildConnectButton(DiscoveredDevice device, bool isConnecting) {
    return ElevatedButton(
      onPressed: isConnecting ? null : () => _connectToDevice(device),
      style: ElevatedButton.styleFrom(
        backgroundColor: Theme.of(context).colorScheme.primary,
        foregroundColor: Colors.white,
        padding: EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8),
        ),
      ),
      child: isConnecting
        ? SizedBox(
            width: 16,
            height: 16,
            child: CircularProgressIndicator(
              strokeWidth: 2,
              valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
            ),
          )
        : Text('Connect'),
    );
  }

  Widget _buildIncomingConnectionActions(DiscoveredDevice device) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        TextButton(
          onPressed: () => _rejectConnection(device),
          child: Text('Decline'),
        ),
        SizedBox(width: 8),
        ElevatedButton(
          onPressed: () => _acceptConnection(device),
          style: ElevatedButton.styleFrom(
            backgroundColor: Colors.green,
            foregroundColor: Colors.white,
          ),
          child: Text('Accept'),
        ),
      ],
    );
  }

  void _connectToDevice(DiscoveredDevice device) async {
    setState(() {
      _connectingEndpointId = device.endpointId;
    });

    final nativeBridge = Provider.of<NativeBridgeService>(context, listen: false);
    await nativeBridge.requestConnect(device.endpointId);

    // Simulate connection process
    await Future.delayed(Duration(seconds: 2));

    if (mounted) {
      setState(() {
        _connectingEndpointId = null;
      });

      // Navigate to chat screen
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(builder: (context) => ChatScreen()),
      );
    }
  }

  void _acceptConnection(DiscoveredDevice device) async {
    final nativeBridge = Provider.of<NativeBridgeService>(context, listen: false);
    await nativeBridge.acceptConnection(device.endpointId);

    setState(() {
      _incomingConnectionEndpointId = null;
      _incomingConnectionDeviceName = null;
    });

    // Navigate to chat screen
    Navigator.of(context).pushReplacement(
      MaterialPageRoute(builder: (context) => ChatScreen()),
    );
  }

  void _rejectConnection(DiscoveredDevice device) async {
    final nativeBridge = Provider.of<NativeBridgeService>(context, listen: false);
    await nativeBridge.rejectConnection(device.endpointId);

    setState(() {
      _incomingConnectionEndpointId = null;
      _incomingConnectionDeviceName = null;
    });
  }

  void _stopScanning() {
    setState(() {
      _isScanning = false;
    });

    final nativeBridge = Provider.of<NativeBridgeService>(context, listen: false);
    nativeBridge.stopDiscovery();
  }

  @override
  void dispose() {
    _stopScanning();
    super.dispose();
  }
}
