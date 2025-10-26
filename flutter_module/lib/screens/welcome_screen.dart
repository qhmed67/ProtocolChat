import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/native_bridge_service.dart';
import 'user_name_screen.dart';

class WelcomeScreen extends StatefulWidget {
  @override
  _WelcomeScreenState createState() => _WelcomeScreenState();
}

class _WelcomeScreenState extends State<WelcomeScreen> with TickerProviderStateMixin {
  bool _isRequestingBluetooth = false;
  bool _isRequestingLocation = false;
  late AnimationController _backgroundController;
  late AnimationController _fadeController;
  late Animation<double> _fadeAnimation;
  late Animation<double> _scaleAnimation;

  @override
  void initState() {
    super.initState();
    _backgroundController = AnimationController(
      duration: Duration(seconds: 8),
      vsync: this,
    )..repeat();
    
    _fadeController = AnimationController(
      duration: Duration(milliseconds: 1200),
      vsync: this,
    );
    
    _fadeAnimation = Tween<double>(
      begin: 0.0,
      end: 1.0,
    ).animate(CurvedAnimation(
      parent: _fadeController,
      curve: Curves.easeOutCubic,
    ));
    
    _scaleAnimation = Tween<double>(
      begin: 0.8,
      end: 1.0,
    ).animate(CurvedAnimation(
      parent: _fadeController,
      curve: Curves.elasticOut,
    ));
    
    _fadeController.forward();
  }

  @override
  void dispose() {
    _backgroundController.dispose();
    _fadeController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              Color(0xFF667eea),
              Color(0xFF764ba2),
              Color(0xFF6B73FF),
              Color(0xFF9B59B6),
            ],
            stops: [0.0, 0.3, 0.7, 1.0],
          ),
        ),
        child: SafeArea(
          child: AnimatedBuilder(
            animation: _backgroundController,
            builder: (context, child) {
              return CustomPaint(
                painter: AnimatedBackgroundPainter(_backgroundController.value),
                child: FadeTransition(
                  opacity: _fadeAnimation,
                  child: ScaleTransition(
                    scale: _scaleAnimation,
                    child: Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 32.0, vertical: 24.0),
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          // Animated App Icon
                          _buildAnimatedIcon(),
                          
                          SizedBox(height: 40),
                          
                          // Welcome Title
                          _buildWelcomeTitle(),
                          
                          SizedBox(height: 16),
                          
                          // Subtitle
                          _buildSubtitle(),
                          
                          SizedBox(height: 12),
                          
                          // Permission Header
                          _buildPermissionHeader(),
                          
                          SizedBox(height: 48),
                          
                          // Permission Buttons
                          _buildPermissionButtons(),
                          
                          SizedBox(height: 32),
                          
                          // Status Indicator
                          _buildStatusIndicator(),
                        ],
                      ),
                    ),
                  ),
                ),
              );
            },
          ),
        ),
      ),
    );
  }

  Widget _buildAnimatedIcon() {
    return AnimatedBuilder(
      animation: _backgroundController,
      builder: (context, child) {
        return Transform.rotate(
          angle: _backgroundController.value * 0.1,
          child: Container(
            width: 140,
            height: 140,
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
                colors: [
                  Colors.white.withOpacity(0.2),
                  Colors.white.withOpacity(0.1),
                ],
              ),
              borderRadius: BorderRadius.circular(35),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.1),
                  blurRadius: 20,
                  offset: Offset(0, 10),
                ),
              ],
            ),
            child: Icon(
              Icons.chat_bubble_outline_rounded,
              size: 70,
              color: Colors.white,
            ),
          ),
        );
      },
    );
  }

  Widget _buildWelcomeTitle() {
    return ShaderMask(
      shaderCallback: (bounds) => LinearGradient(
        colors: [Colors.white, Colors.white.withOpacity(0.8)],
      ).createShader(bounds),
      child: Text(
        'Welcome to ProtocolChat',
        style: TextStyle(
          fontSize: 32,
          fontWeight: FontWeight.w700,
          letterSpacing: -0.5,
          height: 1.2,
        ),
        textAlign: TextAlign.center,
      ),
    );
  }

  Widget _buildSubtitle() {
    return Text(
      'Offline chat powered by Nearby Connections API',
      style: TextStyle(
        fontSize: 16,
        fontWeight: FontWeight.w400,
        color: Colors.white.withOpacity(0.9),
        letterSpacing: 0.2,
      ),
      textAlign: TextAlign.center,
    );
  }

  Widget _buildPermissionHeader() {
    return Container(
      padding: EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.15),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: Colors.white.withOpacity(0.2),
          width: 1,
        ),
      ),
      child: Text(
        'Permissions required',
        style: TextStyle(
          fontSize: 14,
          fontWeight: FontWeight.w600,
          color: Colors.white,
          letterSpacing: 0.5,
        ),
      ),
    );
  }

  Widget _buildPermissionButtons() {
    return Column(
      children: [
        // Bluetooth Button
        Consumer<NativeBridgeService>(
          builder: (context, nativeBridge, child) {
            return _buildPermissionButton(
              icon: Icons.bluetooth_rounded,
              title: 'Grant Bluetooth',
              subtitle: 'Enable device discovery',
              isLoading: _isRequestingBluetooth,
              isCompleted: nativeBridge.bluetoothPermissionGranted && nativeBridge.bluetoothEnabled,
              onTap: () => _handleBluetoothPermission(),
            );
          },
        ),
        
        SizedBox(height: 16),
        
        // Location Button
        Consumer<NativeBridgeService>(
          builder: (context, nativeBridge, child) {
            return _buildPermissionButton(
              icon: Icons.location_on_rounded,
              title: 'Grant Location',
              subtitle: 'Enable nearby device detection',
              isLoading: _isRequestingLocation,
              isCompleted: nativeBridge.locationPermissionGranted && nativeBridge.locationSettingsEnabled,
              onTap: () => _handleLocationPermission(),
            );
          },
        ),
      ],
    );
  }

  Widget _buildPermissionButton({
    required IconData icon,
    required String title,
    required String subtitle,
    required bool isLoading,
    required bool isCompleted,
    required VoidCallback onTap,
  }) {
    return AnimatedContainer(
      duration: Duration(milliseconds: 300),
      curve: Curves.easeInOut,
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: isLoading ? null : onTap,
          borderRadius: BorderRadius.circular(20),
          child: AnimatedContainer(
            duration: Duration(milliseconds: 200),
            padding: EdgeInsets.all(20),
            decoration: BoxDecoration(
              gradient: isCompleted
                  ? LinearGradient(
                      colors: [Color(0xFF4CAF50), Color(0xFF45A049)],
                    )
                  : LinearGradient(
                      colors: [
                        Colors.white.withOpacity(0.2),
                        Colors.white.withOpacity(0.1),
                      ],
                    ),
              borderRadius: BorderRadius.circular(20),
              border: Border.all(
                color: isCompleted
                    ? Colors.green.withOpacity(0.3)
                    : Colors.white.withOpacity(0.2),
                width: 1.5,
              ),
              boxShadow: [
                BoxShadow(
                  color: isCompleted
                      ? Colors.green.withOpacity(0.3)
                      : Colors.black.withOpacity(0.1),
                  blurRadius: isCompleted ? 15 : 10,
                  offset: Offset(0, 5),
                ),
              ],
            ),
            child: Row(
              children: [
                AnimatedContainer(
                  duration: Duration(milliseconds: 200),
                  width: 50,
                  height: 50,
                  decoration: BoxDecoration(
                    color: isCompleted
                        ? Colors.white.withOpacity(0.2)
                        : Colors.white.withOpacity(0.15),
                    borderRadius: BorderRadius.circular(15),
                  ),
                  child: isLoading
                      ? SizedBox(
                          width: 24,
                          height: 24,
                          child: CircularProgressIndicator(
                            strokeWidth: 2.5,
                            valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                          ),
                        )
                      : Icon(
                          isCompleted ? Icons.check_rounded : icon,
                          color: Colors.white,
                          size: 24,
                        ),
                ),
                SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        title,
                        style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.w600,
                          color: Colors.white,
                          letterSpacing: 0.2,
                        ),
                      ),
                      SizedBox(height: 4),
                      Text(
                        subtitle,
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w400,
                          color: Colors.white.withOpacity(0.8),
                          letterSpacing: 0.1,
                        ),
                      ),
                    ],
                  ),
                ),
                if (isCompleted)
                  Icon(
                    Icons.check_circle_rounded,
                    color: Colors.white,
                    size: 24,
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildStatusIndicator() {
    return Consumer<NativeBridgeService>(
      builder: (context, nativeBridge, child) {
        if (nativeBridge.allPermissionsGranted) {
          // Auto-navigate after a short delay
          Future.delayed(Duration(seconds: 2), () {
            if (mounted) {
              Navigator.of(context).pushReplacement(
                MaterialPageRoute(builder: (context) => UserNameScreen()),
              );
            }
          });
          
          return AnimatedContainer(
            duration: Duration(milliseconds: 500),
            padding: EdgeInsets.all(20),
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: [
                  Color(0xFF4CAF50).withOpacity(0.2),
                  Color(0xFF45A049).withOpacity(0.1),
                ],
              ),
              borderRadius: BorderRadius.circular(16),
              border: Border.all(
                color: Color(0xFF4CAF50).withOpacity(0.3),
                width: 1,
              ),
            ),
            child: Row(
              children: [
                Icon(
                  Icons.check_circle_rounded,
                  color: Color(0xFF4CAF50),
                  size: 24,
                ),
                SizedBox(width: 12),
                Expanded(
                  child: Text(
                    'All permissions granted! Proceeding to next step...',
                    style: TextStyle(
                      color: Color(0xFF2E7D32),
                      fontWeight: FontWeight.w600,
                      fontSize: 14,
                    ),
                  ),
                ),
              ],
            ),
          );
        }
        return SizedBox.shrink();
      },
    );
  }

  Future<void> _handleBluetoothPermission() async {
    setState(() {
      _isRequestingBluetooth = true;
    });

    final nativeBridge = Provider.of<NativeBridgeService>(context, listen: false);
    
    await nativeBridge.requestBluetoothPermission();
    await Future.delayed(Duration(milliseconds: 800));
    await nativeBridge.showBluetoothDialog();
    
    if (mounted) {
      setState(() {
        _isRequestingBluetooth = false;
      });
    }
  }

  Future<void> _handleLocationPermission() async {
    setState(() {
      _isRequestingLocation = true;
    });

    final nativeBridge = Provider.of<NativeBridgeService>(context, listen: false);
    
    await nativeBridge.requestLocationPermission();
    await Future.delayed(Duration(milliseconds: 800));
    await nativeBridge.showLocationDialog();
    
    if (mounted) {
      setState(() {
        _isRequestingLocation = false;
      });
    }
  }
}

class AnimatedBackgroundPainter extends CustomPainter {
  final double animationValue;

  AnimatedBackgroundPainter(this.animationValue);

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..shader = LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: [
          Colors.white.withOpacity(0.1),
          Colors.white.withOpacity(0.05),
        ],
      ).createShader(Rect.fromLTWH(0, 0, size.width, size.height));

    // Animated circles
    for (int i = 0; i < 3; i++) {
      final progress = (animationValue + i * 0.3) % 1.0;
      final radius = 100 + i * 50;
      final x = size.width * (0.2 + i * 0.3) + 50 * math.sin(progress * 2 * math.pi);
      final y = size.height * (0.3 + i * 0.2) + 30 * math.cos(progress * 2 * math.pi);
      
      canvas.drawCircle(
        Offset(x, y),
        radius * (0.5 + 0.5 * math.sin(progress * 2 * math.pi)),
        paint,
      );
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => true;
}
