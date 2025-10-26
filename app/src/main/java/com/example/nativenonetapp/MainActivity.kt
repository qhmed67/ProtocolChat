package com.example.nativenonetapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import androidx.activity.result.IntentSenderRequest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class MainActivity : AppCompatActivity() {
    
    private lateinit var nearbyManager: NearbyConnectionsManager
    
    // UI Elements
    private lateinit var tvStatus: TextView
    private lateinit var tvStatusSubtitle: TextView
    private lateinit var ivStatusIcon: ImageView
    private lateinit var ivBluetoothStatus: ImageView
    private lateinit var ivLocationStatus: ImageView
    
    // Permission states
    private var bluetoothPermissionGranted = false
    private var locationPermissionGranted = false
    private var bluetoothEnabled = false
    private var locationEnabled = false
    
    // Permission request launchers
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationPermissionGranted = permissions.values.all { it }
        updateLocationStatus()
        checkAllPermissions()
    }
    
    // Bluetooth enable request launcher
    private val bluetoothEnableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        bluetoothEnabled = result.resultCode == RESULT_OK
        updateBluetoothStatus()
        checkAllPermissions()
    }
    
    // Location settings request launcher
    private val locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        locationEnabled = result.resultCode == RESULT_OK
        updateLocationStatus()
        checkAllPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize Nearby Connections manager
        nearbyManager = NearbyConnectionsManager.getInstance(this, null)
        
        // Initialize UI elements
        initializeViews()
        
        // Wire up buttons
        findViewById<android.widget.Button>(R.id.btnRequestLocation).setOnClickListener {
            requestLocationPermissions()
        }
        
        findViewById<android.widget.Button>(R.id.btnShowBluetoothDialog).setOnClickListener {
            showBluetoothEnableDialog()
        }
        
        findViewById<android.widget.Button>(R.id.btnShowLocationDialog).setOnClickListener {
            showLocationAccuracyDialog()
        }
        
        
        // Check initial permissions
        checkAllPermissions()
    }
    
    private fun initializeViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvStatusSubtitle = findViewById(R.id.tvStatusSubtitle)
        ivStatusIcon = findViewById(R.id.ivStatusIcon)
        ivBluetoothStatus = findViewById(R.id.ivBluetoothStatus)
        ivLocationStatus = findViewById(R.id.ivLocationStatus)
    }
    
    private fun checkAllPermissions() {
        // Use BluetoothCompatibilityHelper for better Android 13-15 support
        bluetoothPermissionGranted = BluetoothCompatibilityHelper.hasAllBluetoothPermissions(this)
        val wifiPermissionGranted = BluetoothCompatibilityHelper.hasWifiPermissions(this)
        bluetoothEnabled = BluetoothCompatibilityHelper.isBluetoothEnabled(this)
        
        // Log compatibility info for debugging
        BluetoothCompatibilityHelper.logBluetoothCompatibilityInfo(this)
        
        // Check if we need to request Bluetooth permissions first
        if (!bluetoothPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestBluetoothPermissions()
        }
        
        // Check Location permission
        locationPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        // Check Location enabled
        locationEnabled = isLocationEnabled()
        
        // Update UI
        updateBluetoothStatus()
        updateLocationStatus()
        
        // Check if all permissions are granted
        val allGranted = bluetoothPermissionGranted && locationPermissionGranted && wifiPermissionGranted && bluetoothEnabled && locationEnabled
        
        if (allGranted) {
            updateStatus("All permissions granted!", "Ready to start chatting", true)
            // Automatically navigate to username screen after a short delay
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                navigateToUsernameScreen()
            }, 2000) // 2 second delay
        } else {
            val errorMessage = BluetoothCompatibilityHelper.getBluetoothErrorMessage(this)
            updateStatus("Grant permissions to start chatting", errorMessage, false)
        }
    }
    
    private fun updateBluetoothStatus() {
        if (bluetoothPermissionGranted && bluetoothEnabled) {
            ivBluetoothStatus.visibility = View.VISIBLE
        } else {
            ivBluetoothStatus.visibility = View.GONE
        }
    }
    
    private fun updateLocationStatus() {
        if (locationPermissionGranted && locationEnabled) {
            ivLocationStatus.visibility = View.VISIBLE
        } else {
            ivLocationStatus.visibility = View.GONE
        }
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
    
    private fun isLocationEnabled(): Boolean {
        return try {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            false
        }
    }
    
    private fun navigateToUsernameScreen() {
        val intent = Intent(this, UsernameActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun requestBluetoothPermissions() {
        val permissions = mutableListOf<String>()
        
        // Get required Bluetooth permissions for current Android version
        permissions.addAll(BluetoothCompatibilityHelper.getRequiredBluetoothPermissions())
        
        // Add additional permissions for Android 13+
        permissions.addAll(BluetoothCompatibilityHelper.getAdditionalPermissions())
        
        if (permissions.isNotEmpty()) {
            val permissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val allGranted = permissions.values.all { it }
                bluetoothPermissionGranted = allGranted
                updateBluetoothStatus()
                checkAllPermissions()
            }
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
    
    private fun requestLocationPermissions() {
        val permissions = mutableListOf<String>()
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        
        if (permissions.isNotEmpty()) {
            locationPermissionLauncher.launch(permissions.toTypedArray())
        }
    }
    
    private fun showBluetoothEnableDialog() {
        try {
            // Check if we have Bluetooth permissions first
            if (!bluetoothPermissionGranted) {
                updateStatus("Bluetooth permissions needed", "Grant Bluetooth permissions first", false)
                requestBluetoothPermissions()
                return
            }
            
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            
            if (bluetoothAdapter == null) {
                updateStatus("Bluetooth not supported", "This device doesn't support Bluetooth", false)
                return
            }
            
            if (!bluetoothAdapter.isEnabled) {
                // For Android 15+, we need to handle potential crashes more gracefully
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    try {
                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        bluetoothEnableLauncher.launch(enableBtIntent)
                    } catch (e: SecurityException) {
                        Log.e("MainActivity", "Security exception when enabling Bluetooth", e)
                        updateStatus("Bluetooth permission denied", "Please grant Bluetooth permissions in settings", false)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error enabling Bluetooth on Android 15+", e)
                        updateStatus("Bluetooth error", "Please enable Bluetooth manually in settings", false)
                    }
                } else {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    bluetoothEnableLauncher.launch(enableBtIntent)
                }
            } else {
                updateStatus("Bluetooth already enabled", "Bluetooth is already turned on", true)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error showing Bluetooth dialog", e)
            updateStatus("Bluetooth error", "Unable to access Bluetooth settings", false)
        }
    }
    
    private fun showLocationAccuracyDialog() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(5000)
            .setMaxUpdateDelayMillis(10000)
            .build()
        
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)
        
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())
        
        task.addOnSuccessListener { locationSettingsResponse ->
            // Location settings are satisfied
        }
        
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    locationSettingsLauncher.launch(IntentSenderRequest.Builder(exception.resolution).build())
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Handle error
                }
            }
        }
    }
    
    
    override fun onDestroy() {
        super.onDestroy()
        nearbyManager.cleanup()
    }
}