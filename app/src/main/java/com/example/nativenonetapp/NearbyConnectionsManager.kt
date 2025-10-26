package com.example.nativenonetapp

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap

class NearbyConnectionsManager private constructor(
    private val context: Context,
    private val channel: Any? = null
) {
    
    // Callback interface for message receiving
    interface MessageCallback {
        fun onMessageReceived(endpointId: String, message: String)
        fun onFileReceived(endpointId: String, fileName: String, filePath: String)
    }
    
    private var messageCallback: MessageCallback? = null
    
    fun setMessageCallback(callback: MessageCallback?) {
        this.messageCallback = callback
    }
    
    companion object {
        private const val TAG = "NearbyConnectionsManager"
        private const val SERVICE_ID = "com.example.nativenonetapp.chat"
        
        @Volatile
        private var INSTANCE: NearbyConnectionsManager? = null
        
        fun getInstance(context: Context, channel: Any? = null): NearbyConnectionsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NearbyConnectionsManager(context.applicationContext, channel).also { INSTANCE = it }
            }
        }
    }
    
    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val discoveredEndpoints = ConcurrentHashMap<String, EndpointInfo>()
    private val connectedEndpoints = ConcurrentHashMap<String, String>()
    private val pendingConnections = ConcurrentHashMap<String, String>() // endpointId to endpointName
    private val tempFiles = mutableListOf<File>()
    private val pendingFilenames = mutableMapOf<String, String>() // endpointId to filename
    
    // Callback for discovered endpoints
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, discoveredEndpointInfo: DiscoveredEndpointInfo) {
            Log.d(TAG, "Endpoint found: $endpointId, name: ${discoveredEndpointInfo.endpointName}")
            discoveredEndpoints[endpointId] = EndpointInfo(
                endpointId = endpointId,
                endpointName = discoveredEndpointInfo.endpointName,
                serviceId = discoveredEndpointInfo.serviceId
            )
            // Device discovered - will be picked up by periodic check
        }
        
        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Endpoint lost: $endpointId")
            discoveredEndpoints.remove(endpointId)
            // Device lost - will be picked up by periodic check
        }
    }
    
    // Callback for connection requests
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.d(TAG, "Connection initiated with: $endpointId from ${connectionInfo.endpointName}")
            // Store pending connection info for approval
            pendingConnections[endpointId] = connectionInfo.endpointName
        }
        
        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d(TAG, "Connection successful: $endpointId")
                    // Get the username from pending connections or discovered endpoints
                    val username = pendingConnections[endpointId] ?: discoveredEndpoints[endpointId]?.endpointName ?: endpointId
                    connectedEndpoints[endpointId] = username
                    pendingConnections.remove(endpointId)
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.d(TAG, "Connection rejected: $endpointId")
                    pendingConnections.remove(endpointId)
                }
                else -> {
                    Log.d(TAG, "Connection failed: $endpointId, status: ${result.status.statusCode}")
                    pendingConnections.remove(endpointId)
                }
            }
        }
        
        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected: $endpointId")
            connectedEndpoints.remove(endpointId)
            pendingConnections.remove(endpointId)
        }
    }
    
    // Callback for payload transfers
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.BYTES -> {
                    val message = String(payload.asBytes()!!)
                    Log.d(TAG, "Message received from $endpointId: $message")
                    
                    // Check if this is a filename message
                    if (message.startsWith("FILE_NAME:")) {
                        val fileName = message.substringAfter("FILE_NAME:")
                        pendingFilenames[endpointId] = fileName
                        Log.d(TAG, "Filename received from $endpointId: $fileName")
                    } else {
                        // Notify UI about received message
                        messageCallback?.onMessageReceived(endpointId, message)
                    }
                }
                Payload.Type.FILE -> {
                    handleFilePayload(endpointId, payload)
                }
                Payload.Type.STREAM -> {
                    Log.d(TAG, "Stream payload received from $endpointId")
                }
            }
        }
        
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            when (update.status) {
                PayloadTransferUpdate.Status.SUCCESS -> {
                    Log.d(TAG, "Payload transfer successful: $endpointId")
                }
                PayloadTransferUpdate.Status.FAILURE -> {
                    Log.d(TAG, "Payload transfer failed: $endpointId")
                }
                PayloadTransferUpdate.Status.IN_PROGRESS -> {
                    Log.d(TAG, "Payload transfer in progress: $endpointId, ${update.bytesTransferred}/${update.totalBytes}")
                }
            }
        }
    }
    
    fun startAdvertising(displayName: String) {
        Log.d(TAG, "Starting advertising with name: $displayName and service ID: $SERVICE_ID")
        
        try {
            val advertisingOptions = AdvertisingOptions.Builder()
                .setStrategy(Strategy.P2P_CLUSTER)
                .build()
            
            connectionsClient.startAdvertising(
                displayName,
                SERVICE_ID,
                connectionLifecycleCallback,
                advertisingOptions
            ).addOnSuccessListener {
                Log.d(TAG, "Advertising started successfully for: $displayName")
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to start advertising for: $displayName", e)
                // Handle specific Android 15+ Bluetooth issues
                if (e.message?.contains("Bluetooth") == true || e.message?.contains("permission") == true) {
                    Log.e(TAG, "Bluetooth permission or availability issue detected", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while starting advertising", e)
        }
    }
    
    fun startDiscovery() {
        Log.d(TAG, "Starting discovery with service ID: $SERVICE_ID")
        
        try {
            val discoveryOptions = DiscoveryOptions.Builder()
                .setStrategy(Strategy.P2P_CLUSTER)
                .build()
            
            connectionsClient.startDiscovery(
                SERVICE_ID,
                endpointDiscoveryCallback,
                discoveryOptions
            ).addOnSuccessListener {
                Log.d(TAG, "Discovery started successfully")
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to start discovery", e)
                // Handle specific Android 15+ Bluetooth scanning issues
                if (e.message?.contains("Bluetooth") == true || e.message?.contains("scan") == true) {
                    Log.e(TAG, "Bluetooth scanning issue detected - common on Android 15", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while starting discovery", e)
        }
    }
    
    fun stopDiscovery() {
        Log.d(TAG, "Stopping discovery")
        connectionsClient.stopDiscovery()
        discoveredEndpoints.clear()
    }
    
    fun requestConnection(endpointId: String, displayName: String) {
        Log.d(TAG, "Requesting connection to: $endpointId")
        
        val connectionOptions = ConnectionOptions.Builder()
            .build()
            
        connectionsClient.requestConnection(
            displayName, // Local endpoint name
            endpointId,
            connectionLifecycleCallback,
            connectionOptions
        ).addOnSuccessListener {
            Log.d(TAG, "Connection request sent to: $endpointId")
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to request connection to: $endpointId", e)
        }
    }
    
    fun acceptConnection(endpointId: String) {
        Log.d(TAG, "Accepting connection from: $endpointId")
        
        val connectionOptions = ConnectionOptions.Builder()
            .build()
            
        connectionsClient.acceptConnection(endpointId, payloadCallback)
            .addOnSuccessListener {
                Log.d(TAG, "Connection accepted: $endpointId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to accept connection: $endpointId", e)
            }
    }
    
    fun rejectConnection(endpointId: String) {
        Log.d(TAG, "Rejecting connection from: $endpointId")
        connectionsClient.rejectConnection(endpointId)
    }
    
    fun sendMessage(endpointId: String, message: String) {
        if (!connectedEndpoints.containsKey(endpointId)) {
            Log.w(TAG, "Not connected to $endpointId to send message to")
            return
        }
        
        val payload = Payload.fromBytes(message.toByteArray())
        
        connectionsClient.sendPayload(endpointId, payload)
            .addOnSuccessListener {
                Log.d(TAG, "Message sent to: $endpointId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to send message to: $endpointId", e)
            }
    }
    
    fun sendAttachment(filePath: String, fileName: String? = null) {
        if (connectedEndpoints.isEmpty()) {
            Log.w(TAG, "No connected endpoints to send attachment to")
            return
        }
        
        val file = File(filePath)
        if (!file.exists()) {
            Log.e(TAG, "File does not exist: $filePath")
            return
        }
        
        // First send the filename as a message
        val actualFileName = fileName ?: file.name
        val filenameMessage = "FILE_NAME:$actualFileName"
        val filenamePayload = Payload.fromBytes(filenameMessage.toByteArray())
        
        // Then send the file
        val filePayload = Payload.fromFile(file)
        
        connectedEndpoints.keys.forEach { endpointId ->
            // Send filename first
            connectionsClient.sendPayload(endpointId, filenamePayload)
                .addOnSuccessListener {
                    Log.d(TAG, "Filename sent to: $endpointId")
                    // Then send the file
                    connectionsClient.sendPayload(endpointId, filePayload)
                        .addOnSuccessListener {
                            Log.d(TAG, "Attachment sent to: $endpointId")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to send attachment to: $endpointId", e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to send filename to: $endpointId", e)
                }
        }
    }
    
    fun disconnect() {
        Log.d(TAG, "Disconnecting from all endpoints")
        connectionsClient.stopAllEndpoints()
        connectedEndpoints.clear()
        cleanupTempFiles()
    }
    
    private fun handleFilePayload(endpointId: String, payload: Payload) {
        val file = payload.asFile()
        if (file != null) {
            try {
                // Get the filename from pending filenames or generate one
                val fileName = pendingFilenames.remove(endpointId) ?: "attachment_${System.currentTimeMillis()}"
                
                // Create a temporary file to store the received attachment
                val tempFile = File.createTempFile("received_", "_$fileName", context.cacheDir)
                tempFiles.add(tempFile)
                
                // Copy the received file to our temp location
                val parcelFileDescriptor = file.asParcelFileDescriptor()
                if (parcelFileDescriptor != null) {
                    try {
                        FileInputStream(parcelFileDescriptor.fileDescriptor).use { inputStream ->
                            FileOutputStream(tempFile).use { outputStream ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                var totalBytes = 0L
                                
                                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                    outputStream.write(buffer, 0, bytesRead)
                                    totalBytes += bytesRead
                                }
                                outputStream.flush()
                                
                                Log.d(TAG, "File copied successfully: $totalBytes bytes")
                                
                                // Verify the copied file
                                if (tempFile.length() != totalBytes) {
                                    Log.e(TAG, "File size mismatch: expected $totalBytes, got ${tempFile.length()}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error copying file", e)
                        // Try alternative approach
                        try {
                            val inputStream = FileInputStream(parcelFileDescriptor.fileDescriptor)
                            val outputStream = FileOutputStream(tempFile)
                            inputStream.copyTo(outputStream)
                            inputStream.close()
                            outputStream.close()
                            Log.d(TAG, "File copied using alternative method")
                        } catch (e2: Exception) {
                            Log.e(TAG, "Alternative copy method also failed", e2)
                            return
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to get ParcelFileDescriptor for received file")
                    return
                }
                
                Log.d(TAG, "File received from $endpointId: ${tempFile.absolutePath}")
                
                // Notify UI about received file
                messageCallback?.onFileReceived(endpointId, fileName, tempFile.absolutePath)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle file payload", e)
            }
        }
    }
    
    private fun cleanupTempFiles() {
        tempFiles.forEach { file ->
            try {
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete temp file: ${file.absolutePath}", e)
            }
        }
        tempFiles.clear()
    }
    
    fun getDiscoveredEndpoints(): Map<String, EndpointInfo> {
        return discoveredEndpoints.toMap()
    }
    
    fun getPendingConnections(): Map<String, String> {
        return pendingConnections.toMap()
    }
    
    fun hasPendingConnections(): Boolean {
        return pendingConnections.isNotEmpty()
    }
    
    fun getConnectedEndpoints(): Map<String, String> {
        return connectedEndpoints.toMap()
    }
    
    fun cleanup() {
        Log.d(TAG, "Cleaning up Nearby Connections manager")
        connectionsClient.stopAllEndpoints()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAdvertising()
        connectedEndpoints.clear()
        discoveredEndpoints.clear()
        pendingConnections.clear()
        pendingFilenames.clear()
        cleanupTempFiles()
    }
    
    fun reset() {
        Log.d(TAG, "Resetting Nearby Connections manager singleton")
        cleanup()
        INSTANCE = null
    }
    
    data class EndpointInfo(
        val endpointId: String,
        val endpointName: String,
        val serviceId: String
    )
}