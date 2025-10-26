package com.example.nativenonetapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class UsernameActivity : AppCompatActivity() {
    
    private lateinit var etUsername: EditText
    private lateinit var btnContinue: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvStatusSubtitle: TextView
    private lateinit var ivStatusIcon: ImageView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_username)
        
        initializeViews()
        setupClickListeners()
    }
    
    private fun initializeViews() {
        etUsername = findViewById(R.id.etUsername)
        btnContinue = findViewById(R.id.btnContinue)
        tvStatus = findViewById(R.id.tvStatus)
        tvStatusSubtitle = findViewById(R.id.tvStatusSubtitle)
        ivStatusIcon = findViewById(R.id.ivStatusIcon)
    }
    
    private fun setupClickListeners() {
        btnContinue.setOnClickListener {
            val username = etUsername.text.toString().trim()
            if (username.isNotEmpty()) {
                // Save username and navigate to scan & connect screen
                updateStatus("Username set!", "Ready to discover nearby devices", true)
                
                // Navigate to scan & connect screen after a short delay
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    val intent = Intent(this, ScanConnectActivity::class.java)
                    intent.putExtra("USERNAME", username)
                    startActivity(intent)
                    finish()
                }, 1500)
            } else {
                updateStatus("Please enter a username", "Username is required to start chatting", false)
            }
        }
    }
    
    override fun onBackPressed() {
        // Exit app when user goes back from username screen
        finishAffinity()
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
}
