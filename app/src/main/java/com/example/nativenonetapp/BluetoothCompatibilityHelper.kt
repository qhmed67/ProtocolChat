package com.example.nativenonetapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Helper class to handle Bluetooth compatibility across Android versions 13, 14, and 15
 * Addresses known Bluetooth crashes and scanning issues
 */
object BluetoothCompatibilityHelper {
    
    private const val TAG = "BluetoothCompatibilityHelper"
    
    /**
     * Check if all required Bluetooth permissions are granted for the current Android version
     */
    fun hasAllBluetoothPermissions(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                // Android 12+ requires new Bluetooth permissions
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                // Android 11 and below use legacy permissions
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
    
    /**
     * Check if Android 13+ WiFi permissions are granted
     */
    fun hasWifiPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required for older versions
        }
    }
    
    /**
     * Safely check if Bluetooth is enabled with proper error handling
     */
    fun isBluetoothEnabled(context: Context): Boolean {
        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            bluetoothAdapter?.isEnabled == true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Bluetooth status", e)
            false
        }
    }
    
    /**
     * Get the list of required Bluetooth permissions for the current Android version
     */
    fun getRequiredBluetoothPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                )
            }
            else -> {
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            }
        }
    }
    
    /**
     * Get additional permissions required for Android 13+
     */
    fun getAdditionalPermissions(): Array<String> {
        val permissions = mutableListOf<String>()
        
        // Android 13+ WiFi permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        
        // Android 13+ notification permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        return permissions.toTypedArray()
    }
    
    /**
     * Check if the device is running Android 15 and has known Bluetooth issues
     */
    fun isAndroid15WithBluetoothIssues(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
    }
    
    /**
     * Get a user-friendly error message for Bluetooth issues
     */
    fun getBluetoothErrorMessage(context: Context): String {
        return when {
            !hasAllBluetoothPermissions(context) -> "Bluetooth permissions are required. Please grant all Bluetooth permissions in settings."
            !isBluetoothEnabled(context) -> "Bluetooth is not enabled. Please turn on Bluetooth in your device settings."
            isAndroid15WithBluetoothIssues() -> "Android 15 detected. If you experience Bluetooth issues, try restarting Bluetooth or your device."
            else -> "Bluetooth is not available. Please check your device settings."
        }
    }
    
    /**
     * Log Bluetooth compatibility information for debugging
     */
    fun logBluetoothCompatibilityInfo(context: Context) {
        Log.d(TAG, "=== Bluetooth Compatibility Info ===")
        Log.d(TAG, "Android Version: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
        Log.d(TAG, "Bluetooth Permissions: ${hasAllBluetoothPermissions(context)}")
        Log.d(TAG, "WiFi Permissions: ${hasWifiPermissions(context)}")
        Log.d(TAG, "Bluetooth Enabled: ${isBluetoothEnabled(context)}")
        Log.d(TAG, "Android 15 with Issues: ${isAndroid15WithBluetoothIssues()}")
        Log.d(TAG, "=====================================")
    }
}
