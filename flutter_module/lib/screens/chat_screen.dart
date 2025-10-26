import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:file_picker/file_picker.dart';
import '../services/native_bridge_service.dart';
import '../services/chat_service.dart';
import '../utils/session_cleanup.dart';

class ChatScreen extends StatefulWidget {
  @override
  _ChatScreenState createState() => _ChatScreenState();
}

class _ChatScreenState extends State<ChatScreen> {
  final TextEditingController _messageController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  bool _isSendingAttachment = false;

  @override
  void initState() {
    super.initState();
    _setupNativeEventHandlers();
  }

  void _setupNativeEventHandlers() {
    // This would be set up in the native bridge service
    // For now, we'll simulate receiving messages
    _simulateIncomingMessages();
  }

  void _simulateIncomingMessages() {
    // Simulate receiving a welcome message
    Future.delayed(Duration(seconds: 1), () {
      if (mounted) {
        final chatService = Provider.of<ChatService>(context, listen: false);
        chatService.addMessage(ChatMessage(
          id: 'msg_1',
          content: 'Hello! Connection established successfully.',
          isFromMe: false,
          timestamp: DateTime.now(),
        ));
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Consumer<ChatService>(
          builder: (context, chatService, child) {
            return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  chatService.connectedDeviceName ?? 'Connected Device',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
                ),
                Text(
                  'Connected',
                  style: TextStyle(fontSize: 12, color: Colors.green),
                ),
              ],
            );
          },
        ),
        backgroundColor: Colors.transparent,
        elevation: 0,
        actions: [
          IconButton(
            icon: Icon(Icons.info_outline),
            onPressed: _showConnectionInfo,
          ),
          PopupMenuButton<String>(
            onSelected: (value) {
              if (value == 'disconnect') {
                _disconnect();
              }
            },
            itemBuilder: (context) => [
              PopupMenuItem(
                value: 'disconnect',
                child: Row(
                  children: [
                    Icon(Icons.close, color: Colors.red),
                    SizedBox(width: 8),
                    Text('Disconnect'),
                  ],
                ),
              ),
            ],
          ),
        ],
      ),
      body: Column(
        children: [
          // Messages list
          Expanded(
            child: Consumer<ChatService>(
              builder: (context, chatService, child) {
                if (chatService.messages.isEmpty) {
                  return Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(
                          Icons.chat_bubble_outline,
                          size: 64,
                          color: Theme.of(context).colorScheme.onSurfaceVariant,
                        ),
                        SizedBox(height: 16),
                        Text(
                          'Start a conversation',
                          style: Theme.of(context).textTheme.titleMedium?.copyWith(
                            color: Theme.of(context).colorScheme.onSurfaceVariant,
                          ),
                        ),
                        SizedBox(height: 8),
                        Text(
                          'Send a message or share a file',
                          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                            color: Theme.of(context).colorScheme.onSurfaceVariant,
                          ),
                        ),
                      ],
                    ),
                  );
                }

                return ListView.builder(
                  controller: _scrollController,
                  padding: EdgeInsets.all(16),
                  itemCount: chatService.messages.length,
                  itemBuilder: (context, index) {
                    final message = chatService.messages[index];
                    return _buildMessageBubble(message);
                  },
                );
              },
            ),
          ),
          
          // Message input
          _buildMessageInput(),
        ],
      ),
    );
  }

  Widget _buildMessageBubble(ChatMessage message) {
    return Padding(
      padding: EdgeInsets.only(bottom: 12),
      child: Row(
        mainAxisAlignment: message.isFromMe ? MainAxisAlignment.end : MainAxisAlignment.start,
        children: [
          if (!message.isFromMe) ...[
            CircleAvatar(
              radius: 16,
              backgroundColor: Theme.of(context).colorScheme.primaryContainer,
              child: Icon(
                Icons.person,
                size: 16,
                color: Theme.of(context).colorScheme.primary,
              ),
            ),
            SizedBox(width: 8),
          ],
          Flexible(
            child: Container(
              padding: EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              decoration: BoxDecoration(
                color: message.isFromMe
                  ? Theme.of(context).colorScheme.primary
                  : Theme.of(context).colorScheme.surfaceContainerHighest,
                borderRadius: BorderRadius.circular(18).copyWith(
                  bottomLeft: message.isFromMe ? Radius.circular(18) : Radius.circular(4),
                  bottomRight: message.isFromMe ? Radius.circular(4) : Radius.circular(18),
                ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (message.isAttachment) ...[
                    Icon(
                      Icons.attach_file,
                      size: 16,
                      color: message.isFromMe ? Colors.white : Theme.of(context).colorScheme.onSurfaceVariant,
                    ),
                    SizedBox(height: 4),
                    Text(
                      message.attachmentName ?? 'Attachment',
                      style: TextStyle(
                        fontWeight: FontWeight.w600,
                        color: message.isFromMe ? Colors.white : Theme.of(context).colorScheme.onSurfaceVariant,
                      ),
                    ),
                    SizedBox(height: 4),
                  ],
                  Text(
                    message.content,
                    style: TextStyle(
                      color: message.isFromMe ? Colors.white : Theme.of(context).colorScheme.onSurfaceVariant,
                    ),
                  ),
                  SizedBox(height: 4),
                  Text(
                    _formatTime(message.timestamp),
                    style: TextStyle(
                      fontSize: 11,
                      color: message.isFromMe 
                        ? Colors.white.withOpacity(0.7)
                        : Theme.of(context).colorScheme.onSurfaceVariant.withOpacity(0.7),
                    ),
                  ),
                ],
              ),
            ),
          ),
          if (message.isFromMe) ...[
            SizedBox(width: 8),
            CircleAvatar(
              radius: 16,
              backgroundColor: Theme.of(context).colorScheme.primary,
              child: Icon(
                Icons.person,
                size: 16,
                color: Colors.white,
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildMessageInput() {
    return Container(
      padding: EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surface,
        border: Border(
          top: BorderSide(
            color: Theme.of(context).colorScheme.outline.withOpacity(0.2),
          ),
        ),
      ),
      child: Row(
        children: [
          // Attachment button
          IconButton(
            onPressed: _isSendingAttachment ? null : _pickAttachment,
            icon: _isSendingAttachment
              ? SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : Icon(Icons.attach_file),
          ),
          
          // Message input field
          Expanded(
            child: TextField(
              controller: _messageController,
              decoration: InputDecoration(
                hintText: 'Type a message...',
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(24),
                  borderSide: BorderSide.none,
                ),
                filled: true,
                fillColor: Theme.of(context).colorScheme.surfaceContainerHighest,
                contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              ),
              maxLines: null,
              textInputAction: TextInputAction.send,
              onSubmitted: (_) => _sendMessage(),
            ),
          ),
          
          SizedBox(width: 8),
          
          // Send button
          FloatingActionButton.small(
            onPressed: _messageController.text.trim().isEmpty ? null : _sendMessage,
            child: Icon(Icons.send),
          ),
        ],
      ),
    );
  }

  void _sendMessage() async {
    final message = _messageController.text.trim();
    if (message.isEmpty) return;

    final chatService = Provider.of<ChatService>(context, listen: false);
    final nativeBridge = Provider.of<NativeBridgeService>(context, listen: false);

    // Add message to local chat
    chatService.addMessage(ChatMessage(
      id: 'msg_${DateTime.now().millisecondsSinceEpoch}',
      content: message,
      isFromMe: true,
      timestamp: DateTime.now(),
    ));

    // Clear input
    _messageController.clear();

    // Send via native bridge
    await nativeBridge.sendMessage(message);

    // Scroll to bottom
    _scrollController.animateTo(
      _scrollController.position.maxScrollExtent,
      duration: Duration(milliseconds: 300),
      curve: Curves.easeOut,
    );
  }

  void _pickAttachment() async {
    setState(() {
      _isSendingAttachment = true;
    });

    try {
      final result = await FilePicker.platform.pickFiles(
        type: FileType.any,
        allowMultiple: false,
      );

      if (result != null && result.files.isNotEmpty) {
        final file = result.files.first;
        final chatService = Provider.of<ChatService>(context, listen: false);
        final nativeBridge = Provider.of<NativeBridgeService>(context, listen: false);

        // Add attachment message to local chat
        chatService.addMessage(ChatMessage(
          id: 'msg_${DateTime.now().millisecondsSinceEpoch}',
          content: 'Sent attachment',
          isFromMe: true,
          timestamp: DateTime.now(),
          isAttachment: true,
          attachmentName: file.name,
        ));

        // Send via native bridge
        await nativeBridge.sendAttachment(file.path!);
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Failed to pick file: $e')),
      );
    } finally {
      if (mounted) {
        setState(() {
          _isSendingAttachment = false;
        });
      }
    }
  }

  void _showConnectionInfo() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Connection Info'),
        content: Consumer<ChatService>(
          builder: (context, chatService, child) {
            return Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Connected to: ${chatService.connectedDeviceName ?? 'Unknown'}'),
                SizedBox(height: 8),
                Text('Connection ID: ${chatService.connectedEndpointId ?? 'Unknown'}'),
                SizedBox(height: 8),
                Text('Status: Connected'),
                SizedBox(height: 8),
                Text('Messages: ${chatService.messages.length}'),
              ],
            );
          },
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: Text('Close'),
          ),
        ],
      ),
    );
  }

  void _disconnect() async {
    final nativeBridge = Provider.of<NativeBridgeService>(context, listen: false);
    final chatService = Provider.of<ChatService>(context, listen: false);

    await nativeBridge.disconnect();
    chatService.cleanupSession();
    
    // Clean up temporary files
    await SessionCleanup.cleanupChatData();

    // Show disconnect message
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Disconnected from chat partner'),
        duration: Duration(seconds: 2),
      ),
    );

    // Navigate back to scan screen
    Navigator.of(context).pop();
  }
  
  void _showPartnerLeftDialog() {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        title: Text('Partner Disconnected'),
        content: Text('Your chat partner left the session. To reconnect, return to the first page. Chat is not saved on phone.'),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.of(context).pop(); // Close dialog
              Navigator.of(context).pop(); // Go back to scan screen
            },
            child: Text('OK'),
          ),
        ],
      ),
    );
  }

  String _formatTime(DateTime timestamp) {
    final now = DateTime.now();
    final difference = now.difference(timestamp);

    if (difference.inMinutes < 1) {
      return 'Just now';
    } else if (difference.inHours < 1) {
      return '${difference.inMinutes}m ago';
    } else if (difference.inDays < 1) {
      return '${difference.inHours}h ago';
    } else {
      return '${timestamp.day}/${timestamp.month} ${timestamp.hour}:${timestamp.minute.toString().padLeft(2, '0')}';
    }
  }

  @override
  void dispose() {
    _messageController.dispose();
    _scrollController.dispose();
    super.dispose();
  }
}
