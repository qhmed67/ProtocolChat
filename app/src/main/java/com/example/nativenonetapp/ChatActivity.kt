package com.example.nativenonetapp

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class ChatMessage(
    val text: String,
    val isFromMe: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isAttachment: Boolean = false,
    val filePath: String? = null
)

class ChatActivity : AppCompatActivity(), NearbyConnectionsManager.MessageCallback {

    companion object {
        private const val FILE_PICKER_REQUEST_CODE = 1001
        private const val STORAGE_PERMISSION_REQUEST_CODE = 1002
        private const val MANAGE_STORAGE_PERMISSION_REQUEST_CODE = 1003
    }
    
    private lateinit var tvConnectedUser: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvStatusSubtitle: TextView
    private lateinit var ivStatusIcon: ImageView
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var btnAttach: Button
    private lateinit var btnDisconnect: Button
    
    private lateinit var nearbyManager: NearbyConnectionsManager
    private lateinit var messagesAdapter: MessagesAdapter
    private val messages = mutableListOf<ChatMessage>()
    private var connectedUsername = ""
    private var endpointId = ""
    
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        
        // Get connected device info
        connectedUsername = intent.getStringExtra("connectedDevice") ?: "Unknown"
        endpointId = intent.getStringExtra("endpointId") ?: ""

        android.util.Log.d("ChatActivity", "Starting chat with: $connectedUsername, endpointId: $endpointId")

        // Initialize Nearby Connections manager
        nearbyManager = NearbyConnectionsManager.getInstance(this, null)
        nearbyManager.setMessageCallback(this)
        
        // Verify connection is still active
        val currentConnectedEndpoints = nearbyManager.getConnectedEndpoints()
        android.util.Log.d("ChatActivity", "Connected endpoints at start: $currentConnectedEndpoints")
        
        if (!currentConnectedEndpoints.containsKey(endpointId)) {
            android.util.Log.w("ChatActivity", "Endpoint not found in connected endpoints! This might cause issues.")
        }
        
        initializeViews()
        setupClickListeners()
        setupRecyclerView()
        
        updateStatus("Connected to $connectedUsername", "Start chatting!", true)
        
        // Debug: Log connection info
        android.util.Log.d("ChatActivity", "Connected to: $connectedUsername, Endpoint ID: $endpointId")
        val connectedEndpoints = nearbyManager.getConnectedEndpoints()
        android.util.Log.d("ChatActivity", "Connected endpoints: $connectedEndpoints")
        
        // Start gentle connection monitoring for disconnection detection
        startDisconnectionMonitoring()
    }
    
    private fun initializeViews() {
        tvConnectedUser = findViewById(R.id.tvConnectedUser)
        tvStatus = findViewById(R.id.tvStatus)
        tvStatusSubtitle = findViewById(R.id.tvStatusSubtitle)
        ivStatusIcon = findViewById(R.id.ivStatusIcon)
        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnAttach = findViewById(R.id.btnAttach)
        btnDisconnect = findViewById(R.id.btnDisconnect)
        
        tvConnectedUser.text = "Chatting with $connectedUsername"
    }
    
    private fun setupClickListeners() {
        btnSend.setOnClickListener {
            sendMessage()
        }
        
        btnAttach.setOnClickListener {
            openFilePicker()
        }
        
        btnDisconnect.setOnClickListener {
            disconnect()
        }
    }
    
    private fun setupRecyclerView() {
        messagesAdapter = MessagesAdapter(messages) { filePath, fileName ->
            downloadFile(filePath, fileName)
        }
        rvMessages.layoutManager = LinearLayoutManager(this)
        rvMessages.adapter = messagesAdapter
    }
    
    private fun sendMessage() {
        val messageText = etMessage.text.toString().trim()
        if (messageText.isNotEmpty()) {
            // Check if we're connected
            val connectedEndpoints = nearbyManager.getConnectedEndpoints()
            if (!connectedEndpoints.containsKey(endpointId)) {
                updateStatus("Not connected", "Connection lost to $connectedUsername", false)
                return
            }
            
            // Add message to local list
            val message = ChatMessage(messageText, true)
            messages.add(message)
            messagesAdapter.notifyItemInserted(messages.size - 1)
            rvMessages.scrollToPosition(messages.size - 1)
            
            // Clear input
            etMessage.text.clear()
            
            // Send via Nearby Connections
            nearbyManager.sendMessage(endpointId, messageText)
        }
    }
    
    private fun addReceivedMessage(messageText: String) {
        val receivedMessage = ChatMessage(messageText, false)
        messages.add(receivedMessage)
        messagesAdapter.notifyItemInserted(messages.size - 1)
        rvMessages.scrollToPosition(messages.size - 1)
    }

    private fun addReceivedFile(fileName: String, filePath: String) {
        val attachmentMessage = "📎 $fileName"
        val receivedFile = ChatMessage(attachmentMessage, false, isAttachment = true, filePath = filePath)
        messages.add(receivedFile)
        messagesAdapter.notifyItemInserted(messages.size - 1)
        rvMessages.scrollToPosition(messages.size - 1)
    }
    
    // Implementation of MessageCallback interface
    override fun onMessageReceived(endpointId: String, message: String) {
        // Run on UI thread
        runOnUiThread {
            addReceivedMessage(message)
        }
    }

    override fun onFileReceived(endpointId: String, fileName: String, filePath: String) {
        // Run on UI thread
        runOnUiThread {
            addReceivedFile(fileName, filePath)
        }
    }
    
    private fun startDisconnectionMonitoring() {
        // Gentle monitoring every 5 seconds to detect disconnections
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                try {
                    val connectedEndpoints = nearbyManager.getConnectedEndpoints()
                    android.util.Log.d("ChatActivity", "Checking disconnection: endpointId=$endpointId, connectedEndpoints=$connectedEndpoints")
                    
                    if (!connectedEndpoints.containsKey(endpointId)) {
                        // Other user disconnected - restart session
                        android.util.Log.d("ChatActivity", "Other user disconnected, restarting session...")
                        runOnUiThread {
                            showDisconnectionMessage()
                        }
                    } else {
                        // Still connected, continue monitoring
                        handler.postDelayed(this, 5000)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ChatActivity", "Error in disconnection monitoring", e)
                    // Continue monitoring even if there's an error
                    handler.postDelayed(this, 5000)
                }
            }
        }
        handler.post(runnable)
    }
    
    private fun showDisconnectionMessage() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("User Disconnected")
            .setMessage("$connectedUsername has disconnected. Starting new session...")
            .setCancelable(false)
            .setOnDismissListener {
                restartSession()
            }
            .show()
        
        // Auto-restart after 2 seconds
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            restartSession()
        }, 2000)
    }
    
    private fun openFilePicker() {
        // Request file permissions before opening file picker
        if (checkFilePermissions()) {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(Intent.createChooser(intent, "Select a file"), FILE_PICKER_REQUEST_CODE)
        } else {
            requestFilePermissions()
        }
    }


    private fun handleFileAttachment(uri: Uri) {
        try {
            // Get file info
            val fileName = getFileName(uri)
            val fileSize = getFileSize(uri)
            
            // Check if we're connected
            val connectedEndpoints = nearbyManager.getConnectedEndpoints()
            if (!connectedEndpoints.containsKey(endpointId)) {
                updateStatus("Not connected", "Connection lost to $connectedUsername", false)
                return
            }

            // Copy file to temporary location
            val tempFile = copyUriToTempFile(uri, fileName)
            if (tempFile != null) {
                // Add attachment message to chat
                val attachmentMessage = "📎 $fileName (${formatFileSize(fileSize)})"
                val message = ChatMessage(attachmentMessage, true)
                messages.add(message)
                messagesAdapter.notifyItemInserted(messages.size - 1)
                rvMessages.scrollToPosition(messages.size - 1)

                // Send file via Nearby Connections
                nearbyManager.sendAttachment(tempFile.absolutePath, fileName)
                updateStatus("File sent", "Attachment sent to $connectedUsername", true)
            } else {
                updateStatus("File error", "Could not access selected file", false)
            }
            
        } catch (e: Exception) {
            Log.e("ChatActivity", "Error handling file attachment", e)
            updateStatus("File error", "Failed to send attachment", false)
        }
    }

    private fun getFileName(uri: Uri): String {
        var fileName = "Unknown file"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = it.getString(nameIndex) ?: "Unknown file"
                }
            }
        }
        return fileName
    }

    private fun getFileSize(uri: Uri): Long {
        var fileSize = 0L
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    fileSize = it.getLong(sizeIndex)
                }
            }
        }
        return fileSize
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    private fun copyUriToTempFile(uri: Uri, fileName: String): java.io.File? {
        return try {
            val tempFile = java.io.File(cacheDir, "temp_${System.currentTimeMillis()}_$fileName")
            contentResolver.openInputStream(uri)?.use { inputStream ->
                java.io.FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    outputStream.flush()
                }
            }
            tempFile
        } catch (e: Exception) {
            Log.e("ChatActivity", "Error copying file to temp location", e)
            null
        }
    }

    private fun downloadFile(filePath: String, fileName: String) {
        // Request file permissions before downloading
        if (checkFilePermissions()) {
            performDownload(filePath, fileName)
        } else {
            requestFilePermissions()
        }
    }
    
    private fun performDownload(filePath: String, fileName: String) {
        try {
            val file = java.io.File(filePath)
            if (file.exists() && file.length() > 0) {
                // Validate file integrity
                Log.d("ChatActivity", "File size: ${file.length()} bytes")
                Log.d("ChatActivity", "File path: $filePath")
                Log.d("ChatActivity", "File name: $fileName")
                
                // Try multiple approaches to open the file
                try {
                    // Approach 1: Direct file access
                    val intent = Intent(Intent.ACTION_VIEW)
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        file
                    )
                    intent.setDataAndType(uri, getMimeType(fileName))
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    
                    // Try to open the file
                    val chooser = Intent.createChooser(intent, "Open $fileName")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(chooser)
                    
                } catch (e: Exception) {
                    Log.e("ChatActivity", "Failed to open with FileProvider, trying alternative", e)
                    
                    // Approach 2: Copy to Downloads folder
                    val downloadsDir = getExternalFilesDir(null) ?: filesDir
                    val targetFile = java.io.File(downloadsDir, fileName)
                    
                    // Copy file to Downloads
                    file.copyTo(targetFile, overwrite = true)
                    
                    val intent2 = Intent(Intent.ACTION_VIEW)
                    val uri2 = androidx.core.content.FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        targetFile
                    )
                    intent2.setDataAndType(uri2, getMimeType(fileName))
                    intent2.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    
                    val chooser2 = Intent.createChooser(intent2, "Open $fileName")
                    chooser2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(chooser2)
                }
                
            } else {
                Toast.makeText(this, "File not found or empty", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ChatActivity", "Error opening file", e)
            Toast.makeText(this, "Could not open file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.').lowercase()
        return when (extension) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "webp" -> "image/webp"
            "txt" -> "text/plain"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            "mp4" -> "video/mp4"
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            else -> "application/octet-stream"
        }
    }

    private fun disconnect() {
        nearbyManager.disconnect()
        restartSession()
    }
    
    private fun restartSession() {
        // Clean up Nearby Connections before restarting
        try {
            nearbyManager.disconnect()
            nearbyManager.reset() // Complete reset of singleton
        } catch (e: Exception) {
            android.util.Log.e("ChatActivity", "Error cleaning up connections", e)
        }
        
        // Clear all activities and go back to username screen
        val intent = Intent(this, UsernameActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun updateStatus(title: String, subtitle: String, isSuccess: Boolean) {
        tvStatus.text = title
        tvStatusSubtitle.text = subtitle
        
        if (isSuccess) {
            ivStatusIcon.setImageResource(android.R.drawable.ic_dialog_info)
        } else {
            ivStatusIcon.setImageResource(android.R.drawable.ic_dialog_info)
        }
    }
    
    private fun checkFilePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+)
            Environment.isExternalStorageManager()
        } else {
            // Android 10 and below
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestFilePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+) - Request MANAGE_EXTERNAL_STORAGE permission
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, MANAGE_STORAGE_PERMISSION_REQUEST_CODE)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivityForResult(intent, MANAGE_STORAGE_PERMISSION_REQUEST_CODE)
            }
        } else {
            // Android 10 and below - Request traditional storage permissions
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                STORAGE_PERMISSION_REQUEST_CODE
            )
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            STORAGE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Toast.makeText(this, "File permissions granted! You can now upload/download files.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "File permissions denied. File sharing may not work properly.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            FILE_PICKER_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.let { uri ->
                        handleFileAttachment(uri)
                    }
                }
            }
            MANAGE_STORAGE_PERMISSION_REQUEST_CODE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        Toast.makeText(this, "File permissions granted! You can now upload/download files.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "File permissions denied. File sharing may not work properly.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        nearbyManager.cleanup()
    }
}

class MessagesAdapter(
    private val messages: List<ChatMessage>,
    private val onDownloadClick: (String, String) -> Unit
) : RecyclerView.Adapter<MessagesAdapter.MessageViewHolder>() {
    
    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
        val messageContainer: LinearLayout = view.findViewById(R.id.messageContainer)
        val messageBubble: androidx.cardview.widget.CardView = view.findViewById(R.id.messageBubble)
        val rootLayout: LinearLayout = view.findViewById(R.id.rootLayout)
        val btnDownload: Button = view.findViewById(R.id.btnDownload)
    }
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): MessageViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        
        holder.tvMessage.text = message.text
        holder.tvTimestamp.text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(message.timestamp))
        
        // Handle download button for attachments
        if (message.isAttachment && !message.isFromMe && message.filePath != null) {
            holder.btnDownload.visibility = View.VISIBLE
            holder.btnDownload.setOnClickListener {
                onDownloadClick(message.filePath, message.text.substringAfter("📎 "))
            }
        } else {
            holder.btnDownload.visibility = View.GONE
        }
        
        if (message.isFromMe) {
            // My message - align right, blue gradient background
            holder.rootLayout.gravity = android.view.Gravity.END
            holder.messageBubble.setCardBackgroundColor(holder.itemView.context.resources.getColor(android.R.color.transparent, null))
            holder.messageBubble.background = holder.itemView.context.getDrawable(R.drawable.message_bubble_me)
            holder.tvMessage.setTextColor(holder.itemView.context.resources.getColor(android.R.color.white, null))
            holder.tvTimestamp.gravity = android.view.Gravity.END
        } else {
            // Other's message - align left, light gray background
            holder.rootLayout.gravity = android.view.Gravity.START
            holder.messageBubble.setCardBackgroundColor(holder.itemView.context.resources.getColor(android.R.color.transparent, null))
            holder.messageBubble.background = holder.itemView.context.getDrawable(R.drawable.message_bubble_other)
            holder.tvMessage.setTextColor(holder.itemView.context.resources.getColor(android.R.color.black, null))
            holder.tvTimestamp.gravity = android.view.Gravity.START
        }
    }
    
    override fun getItemCount() = messages.size
}
