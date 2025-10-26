package com.example.nativenonetapp

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ConnectionProcessingActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvStatusSubtitle: TextView
    private lateinit var ivStatusIcon: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvCountdown: TextView
    private lateinit var btnCancel: Button
    private lateinit var tvConnectionStatus: TextView
    private lateinit var btnAccept: Button
    private lateinit var btnDecline: Button

    private var countdownTimer: CountDownTimer? = null
    private var targetUsername = ""
    private var endpointId = ""
    private lateinit var nearbyManager: NearbyConnectionsManager
    private var isConnectionSuccessful = false

    companion object {
        private const val CONNECTION_TIMEOUT = 15000L // 15 seconds
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection_processing)

        // Get data from intent
        targetUsername = intent.getStringExtra("targetUsername") ?: "Unknown"
        endpointId = intent.getStringExtra("endpointId") ?: ""

        // Initialize Nearby Connections manager
        nearbyManager = NearbyConnectionsManager.getInstance(this, null)

        initializeViews()
        setupClickListeners()
        startConnectionProcess()
        startConnectionMonitoring()
    }

    private fun initializeViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvStatusSubtitle = findViewById(R.id.tvStatusSubtitle)
        ivStatusIcon = findViewById(R.id.ivStatusIcon)
        progressBar = findViewById(R.id.progressBar)
        tvCountdown = findViewById(R.id.tvCountdown)
        btnCancel = findViewById(R.id.btnCancel)
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus)
        btnAccept = findViewById(R.id.btnAccept)
        btnDecline = findViewById(R.id.btnDecline)
    }

    private fun setupClickListeners() {
        btnCancel.setOnClickListener {
            cancelConnection()
        }
        
        btnAccept.setOnClickListener {
            acceptConnection()
        }
        
        btnDecline.setOnClickListener {
            declineConnection()
        }
    }

    private fun startConnectionProcess() {
        updateStatus("Connecting to $targetUsername", "Waiting for connection...", false)
        
        // No automatic timeout - let user decide when to cancel
        progressBar.visibility = View.VISIBLE
        tvCountdown.text = "Waiting for connection... (No timeout)"
    }

    private fun startConnectionMonitoring() {
        // Check connection status every 500ms
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                if (!isConnectionSuccessful) {
                    val connectedEndpoints = nearbyManager.getConnectedEndpoints()
                    val pendingConnections = nearbyManager.getPendingConnections()
                    
                    android.util.Log.d("ConnectionProcessing", "Connected endpoints: $connectedEndpoints")
                    android.util.Log.d("ConnectionProcessing", "Pending connections: $pendingConnections")
                    android.util.Log.d("ConnectionProcessing", "Looking for endpointId: $endpointId")
                    
                    if (connectedEndpoints.containsKey(endpointId)) {
                        // Connection successful!
                        isConnectionSuccessful = true
                        updateStatus("Connected!", "Successfully connected to $targetUsername", true)
                        
                        android.util.Log.d("ConnectionProcessing", "Connection successful! Navigating to chat...")
                        
                        // Navigate to chat after a short delay
                        handler.postDelayed({
                            navigateToChat()
                        }, 1000)
                    } else if (pendingConnections.containsKey(endpointId)) {
                        // Connection request received - show accept/decline buttons
                        showConnectionRequest()
                    } else {
                        // Still waiting for connection request
                        tvConnectionStatus.text = "Waiting for connection request from $targetUsername..."
                        handler.postDelayed(this, 500)
                    }
                }
            }
        }
        handler.post(runnable)
    }
    
    private fun showConnectionRequest() {
        // Show accept/decline buttons
        tvConnectionStatus.text = "Connection request received from $targetUsername"
        btnAccept.visibility = View.VISIBLE
        btnDecline.visibility = View.VISIBLE
        
        // Update status
        updateStatus("Connection Request", "$targetUsername wants to connect with you", false)
    }

    // Removed countdown timer - users can wait as long as they want

    private fun acceptConnection() {
        // Accept the connection
        nearbyManager.acceptConnection(endpointId)
        
        // Update UI
        tvConnectionStatus.text = "Connection accepted! Establishing secure connection..."
        btnAccept.visibility = View.GONE
        btnDecline.visibility = View.GONE
        
        // Show success status
        updateStatus("Connection Accepted", "Establishing secure connection to $targetUsername", true)
    }
    
    private fun declineConnection() {
        // Decline the connection
        nearbyManager.rejectConnection(endpointId)
        
        // Update UI
        tvConnectionStatus.text = "Connection declined"
        btnAccept.visibility = View.GONE
        btnDecline.visibility = View.GONE
        
        // Show decline status
        updateStatus("Connection Declined", "You declined the connection from $targetUsername", false)
        
        // Go back to scan screen after delay
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, ScanConnectActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }, 2000)
    }

    private fun cancelConnection() {
        // Go back to scan & connect screen
        val intent = Intent(this, ScanConnectActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }

    private fun navigateToChat() {
        android.util.Log.d("ConnectionProcessing", "Navigating to chat with: $targetUsername, endpointId: $endpointId")
        
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("connectedDevice", targetUsername)
        intent.putExtra("endpointId", endpointId)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun updateStatus(title: String, subtitle: String, isSuccess: Boolean) {
        tvStatus.text = title
        tvStatusSubtitle.text = subtitle

        if (isSuccess) {
            ivStatusIcon.setImageResource(R.drawable.ic_check_circle)
        } else {
            ivStatusIcon.setImageResource(android.R.drawable.ic_dialog_info)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()
    }
    
    override fun onBackPressed() {
        // Clean up connections before restarting session
        try {
            nearbyManager.disconnect()
            nearbyManager.reset() // Complete reset of singleton
        } catch (e: Exception) {
            android.util.Log.e("ConnectionProcessingActivity", "Error cleaning up connections", e)
        }
        
        // When user goes back, restart session
        val intent = Intent(this, UsernameActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
