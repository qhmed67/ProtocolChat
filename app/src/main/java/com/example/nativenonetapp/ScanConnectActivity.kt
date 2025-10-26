package com.example.nativenonetapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ScanConnectActivity : AppCompatActivity() {
    
    private lateinit var tvStatus: TextView
    private lateinit var tvStatusSubtitle: TextView
    private lateinit var ivStatusIcon: ImageView
    private lateinit var lvDevices: ListView
    private lateinit var btnStartScan: Button
    private lateinit var btnStopScan: Button
    
    private lateinit var nearbyManager: NearbyConnectionsManager
    private var isScanning = false
    private val discoveredDevices = mutableListOf<DiscoveredDevice>()
    private lateinit var devicesAdapter: ArrayAdapter<DiscoveredDevice>
    private var isAdvertising = false
    private var currentUsername = ""
    private var hasNavigatedToChat = false
    private val shownDialogs = mutableSetOf<String>() // Track shown dialogs by endpointId
    private val initiatedConnections = mutableSetOf<String>() // Track connections we initiated
    
    data class DiscoveredDevice(
        val endpointId: String,
        val username: String,
        val isConnected: Boolean = false,
        val isPending: Boolean = false
    ) {
        override fun toString(): String {
            return when {
                isConnected -> "$username (Connected)"
                isPending -> "$username (Pending...)"
                else -> username
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_connect)
        
        // Get username from intent
        currentUsername = intent.getStringExtra("USERNAME") ?: "Anonymous"
        
        initializeViews()
        setupClickListeners()
        setupListAdapter()
        
        // Initialize Nearby Connections manager
        nearbyManager = NearbyConnectionsManager.getInstance(this, null)
        
        // Start both advertising and scanning automatically
        startAdvertising()
        startScanning()
        
        // Start checking for discovered devices periodically
        startDeviceDiscoveryCheck()
    }
    
    private fun initializeViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvStatusSubtitle = findViewById(R.id.tvStatusSubtitle)
        ivStatusIcon = findViewById(R.id.ivStatusIcon)
        lvDevices = findViewById(R.id.lvDevices)
        btnStartScan = findViewById(R.id.btnStartScan)
        btnStopScan = findViewById(R.id.btnStopScan)
    }
    
    private fun setupClickListeners() {
        btnStartScan.setOnClickListener {
            startScanning()
        }
        
        btnStopScan.setOnClickListener {
            stopScanning()
        }
        
        lvDevices.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val device = discoveredDevices[position]
            if (!device.isConnected) {
                connectToDevice(device)
            }
        }
    }
    
    private fun setupListAdapter() {
        devicesAdapter = object : ArrayAdapter<DiscoveredDevice>(this, R.layout.item_device, discoveredDevices) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val view = convertView ?: layoutInflater.inflate(R.layout.item_device, parent, false)
                val device = getItem(position)!!
                
                val tvUsername = view.findViewById<TextView>(R.id.tvUsername)
                val tvStatus = view.findViewById<TextView>(R.id.tvDeviceStatus)
                val btnConnect = view.findViewById<Button>(R.id.btnConnect)
                val ivDeviceIcon = view.findViewById<ImageView>(R.id.ivDeviceIcon)
                
                tvUsername.text = device.username
                
                if (device.isConnected) {
                    tvStatus.text = "Connected"
                    tvStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                    btnConnect.text = "Chat"
                    btnConnect.isEnabled = true
                    btnConnect.background = resources.getDrawable(R.drawable.button_secondary, null)
                } else {
                    tvStatus.text = "Available"
                    tvStatus.setTextColor(resources.getColor(android.R.color.darker_gray, null))
                    btnConnect.text = "Connect"
                    btnConnect.isEnabled = true
                    btnConnect.background = resources.getDrawable(R.drawable.button_primary, null)
                }
                
                btnConnect.setOnClickListener {
                    if (device.isConnected) {
                        // Navigate to chat
                        navigateToChat(device)
                    } else {
                        // Connect to device
                        connectToDevice(device)
                    }
                }
                
                return view
            }
        }
        lvDevices.adapter = devicesAdapter
    }
    
    private fun startAdvertising() {
        if (!isAdvertising) {
            // Check Bluetooth compatibility before starting advertising
            if (!BluetoothCompatibilityHelper.hasAllBluetoothPermissions(this)) {
                android.util.Log.w("ScanConnectActivity", "Cannot start advertising - Bluetooth permissions not granted")
                return
            }
            
            if (!BluetoothCompatibilityHelper.isBluetoothEnabled(this)) {
                android.util.Log.w("ScanConnectActivity", "Cannot start advertising - Bluetooth not enabled")
                return
            }
            
            isAdvertising = true
            // Start advertising so other devices can find us
            try {
                nearbyManager.startAdvertising(currentUsername)
            } catch (e: Exception) {
                android.util.Log.e("ScanConnectActivity", "Error starting advertising", e)
                isAdvertising = false
            }
        }
    }
    
    private fun startScanning() {
        if (!isScanning) {
            isScanning = true
            updateStatus("Scanning for devices...", "Looking for nearby users to chat with", false)
            
            // Check Bluetooth compatibility before starting discovery
            if (!BluetoothCompatibilityHelper.hasAllBluetoothPermissions(this)) {
                updateStatus("Bluetooth permissions needed", "Please grant Bluetooth permissions first", false)
                isScanning = false
                btnStartScan.isEnabled = true
                btnStopScan.isEnabled = false
                return
            }
            
            if (!BluetoothCompatibilityHelper.isBluetoothEnabled(this)) {
                updateStatus("Bluetooth not enabled", "Please enable Bluetooth first", false)
                isScanning = false
                btnStartScan.isEnabled = true
                btnStopScan.isEnabled = false
                return
            }
            
            // Start Nearby Connections discovery with error handling
            try {
                nearbyManager.startDiscovery()
                btnStartScan.isEnabled = false
                btnStopScan.isEnabled = true
            } catch (e: Exception) {
                android.util.Log.e("ScanConnectActivity", "Error starting discovery", e)
                updateStatus("Scanning failed", "Unable to start device discovery", false)
                isScanning = false
                btnStartScan.isEnabled = true
                btnStopScan.isEnabled = false
            }
        }
    }
    
    private fun stopScanning() {
        if (isScanning) {
            isScanning = false
            nearbyManager.stopDiscovery()
            updateStatus("Scanning stopped", "Tap start to scan for devices again", false)
            
            btnStartScan.isEnabled = true
            btnStopScan.isEnabled = false
        }
    }
    
    private fun addDiscoveredDevice(endpointId: String, username: String) {
        // Check if device already exists
        val existingDevice = discoveredDevices.find { it.endpointId == endpointId }
        if (existingDevice == null) {
            val device = DiscoveredDevice(endpointId, username)
            discoveredDevices.add(device)
            devicesAdapter.notifyDataSetChanged()
            
            updateStatus("Found ${discoveredDevices.size} device(s)", "Tap on a device to connect", true)
        }
    }
    
    private fun removeDiscoveredDevice(endpointId: String) {
        val device = discoveredDevices.find { it.endpointId == endpointId }
        if (device != null) {
            discoveredDevices.remove(device)
            devicesAdapter.notifyDataSetChanged()
            
            updateStatus("Found ${discoveredDevices.size} device(s)", "Tap on a device to connect", true)
        }
    }
    
    private fun startDeviceDiscoveryCheck() {
        // Check for discovered devices every 2 seconds
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                if (isScanning) {
                    // Get discovered endpoints from NearbyConnectionsManager
                    val discoveredEndpoints = nearbyManager.getDiscoveredEndpoints()
                    for ((endpointId, endpointInfo) in discoveredEndpoints) {
                        addDiscoveredDevice(endpointId, endpointInfo.endpointName)
                    }
                    
                    // Check for pending connections (only show dialog if we didn't initiate the connection)
                    val pendingConnections = nearbyManager.getPendingConnections()
                    for ((endpointId, endpointName) in pendingConnections) {
                        if (!shownDialogs.contains(endpointId) && !initiatedConnections.contains(endpointId)) {
                            showConnectionApprovalDialog(endpointId, endpointName)
                            shownDialogs.add(endpointId)
                        }
                    }
                    
                    // Check for successful connections
                    val connectedEndpoints = nearbyManager.getConnectedEndpoints()
                    for ((endpointId, endpointName) in connectedEndpoints) {
                        handleConnectionSuccess(endpointId, endpointName)
                    }
                }
                handler.postDelayed(this, 2000)
            }
        }
        handler.post(runnable)
    }
    
    private fun showConnectionApprovalDialog(endpointId: String, endpointName: String) {
        // Find the actual device name from discovered devices
        val device = discoveredDevices.find { it.endpointId == endpointId }
        val displayName = device?.username ?: endpointName
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Connection Request")
            .setMessage("$displayName wants to connect and chat with you. Do you want to accept?")
            .setPositiveButton("Accept") { _, _ ->
                // Accept the connection
                nearbyManager.acceptConnection(endpointId)
                updateStatus("Connection accepted", "You are now connected to $displayName", true)
            }
            .setNegativeButton("Decline") { _, _ ->
                // Reject the connection
                nearbyManager.rejectConnection(endpointId)
                updateStatus("Connection declined", "Connection request was declined", false)
            }
            .setCancelable(false)
            .create()
        
        dialog.show()
    }
    
    
    private fun connectToDevice(device: DiscoveredDevice) {
        // Update device status to pending
        val index = discoveredDevices.indexOf(device)
        if (index != -1) {
            discoveredDevices[index] = device.copy(isPending = true)
            devicesAdapter.notifyDataSetChanged()
        }
        
        // Track that we initiated this connection
        initiatedConnections.add(device.endpointId)
        
        // Initiate Nearby Connections request
        nearbyManager.requestConnection(device.endpointId, currentUsername)
        
        // Navigate to connection processing page
        val intent = Intent(this, ConnectionProcessingActivity::class.java)
        intent.putExtra("targetUsername", device.username)
        intent.putExtra("endpointId", device.endpointId)
        startActivity(intent)
    }
    
    private fun handleConnectionSuccess(endpointId: String, endpointName: String) {
        // Prevent multiple navigations
        if (hasNavigatedToChat) return
        
        // Find the device and mark it as connected
        val device = discoveredDevices.find { it.endpointId == endpointId }
        if (device != null) {
            val index = discoveredDevices.indexOf(device)
            discoveredDevices[index] = device.copy(isConnected = true, isPending = false)
            devicesAdapter.notifyDataSetChanged()
            
            updateStatus("Connected to $endpointName!", "Ready to start chatting", true)
            
            // Set flag to prevent multiple navigations
            hasNavigatedToChat = true
            
            // Navigate to chat after a short delay
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                navigateToChat(device)
            }, 1500)
        }
    }
    
    private fun navigateToChat(device: DiscoveredDevice) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("connectedDevice", device.username)
        intent.putExtra("endpointId", device.endpointId)
        startActivity(intent)
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
    
    override fun onDestroy() {
        super.onDestroy()
        nearbyManager.cleanup()
        // Reset flags for next time
        hasNavigatedToChat = false
        shownDialogs.clear()
    }
    
    override fun onBackPressed() {
        // Clean up connections before restarting session
        try {
            nearbyManager.disconnect()
            nearbyManager.reset() // Complete reset of singleton
        } catch (e: Exception) {
            android.util.Log.e("ScanConnectActivity", "Error cleaning up connections", e)
        }
        
        // When user goes back, restart session
        val intent = Intent(this, UsernameActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
