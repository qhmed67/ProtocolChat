import 'dart:io';
import 'package:path_provider/path_provider.dart';

class SessionCleanup {
  static Future<void> cleanupTempFiles() async {
    try {
      final tempDir = await getTemporaryDirectory();
      final cacheDir = await getApplicationCacheDirectory();
      
      // Clean up temporary files in cache directory
      await _cleanupDirectory(cacheDir);
      
      // Clean up temporary files in temp directory
      await _cleanupDirectory(tempDir);
      
      print('Session cleanup completed successfully');
    } catch (e) {
      print('Error during session cleanup: $e');
    }
  }
  
  static Future<void> _cleanupDirectory(Directory directory) async {
    try {
      if (await directory.exists()) {
        final files = await directory.list().toList();
        
        for (final file in files) {
          if (file is File) {
            // Delete files that look like temporary chat files
            final fileName = file.path.split('/').last;
            if (fileName.startsWith('attachment_') || 
                fileName.startsWith('chat_temp_') ||
                fileName.startsWith('nearby_temp_')) {
              try {
                await file.delete();
                print('Deleted temp file: $fileName');
              } catch (e) {
                print('Could not delete file $fileName: $e');
              }
            }
          }
        }
      }
    } catch (e) {
      print('Error cleaning directory ${directory.path}: $e');
    }
  }
  
  static Future<void> cleanupChatData() async {
    // This method can be extended to clean up any persistent chat data
    // For now, we rely on the ephemeral nature of the chat service
    await cleanupTempFiles();
  }
}
