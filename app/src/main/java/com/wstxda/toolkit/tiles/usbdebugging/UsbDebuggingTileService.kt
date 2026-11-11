package com.wstxda.toolkit.tiles.usbdebugging

import android.content.Context
import android.provider.Settings
import android.service.quicksettings.Tile
import com.wstxda.toolkit.R
import com.wstxda.toolkit.activity.WriteSecureSettingsActivity
import com.wstxda.toolkit.base.BaseTileService

/**
 * Requests USB debugging and Wireless debugging to be enabled/disabled together.
 *
 * Android 11 does not expose a reliable public read-back API for USB debugging
 * state to ordinary apps. The tile therefore displays the last requested state,
 * explicitly marked as unverified.
 */
class UsbDebuggingTileService : BaseTileService() {

    private val preferences by lazy {
        getSharedPreferences("adb_control_state", Context.MODE_PRIVATE)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        if (checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            startActivityAndCollapse(WriteSecureSettingsActivity::class.java)
            return
        }

        val currentlyRequested = preferences.getBoolean(KEY_REQUESTED_ENABLED, false)
        val requested = !currentlyRequested

        val usbWritten = writeGlobalSetting(Settings.Global.ADB_ENABLED, requested)
        val wirelessWritten = writeGlobalSetting(ADB_WIFI_ENABLED, requested)

        // Store intent, not verified device state. Android 11/MIUI may ignore
        // or restrict one of these settings even when putInt reports success.
        preferences.edit().putBoolean(KEY_REQUESTED_ENABLED, requested).apply()

        updateTile()
    }

    private fun writeGlobalSetting(name: String, enabled: Boolean): Boolean {
        return try {
            Settings.Global.putInt(contentResolver, name, if (enabled) 1 else 0)
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    override fun updateTile() {
        val hasPermission = checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        val requested = preferences.getBoolean(KEY_REQUESTED_ENABLED, false)

        setTileState(
            state = if (requested && hasPermission) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE,
            label = getString(R.string.adb_control_tile),
            subtitle = when {
                !hasPermission -> getString(R.string.tile_setup)
                requested -> getString(R.string.adb_control_requested_on)
                else -> getString(R.string.adb_control_requested_off)
            },
            icon = android.graphics.drawable.Icon.createWithResource(
                this,
                if (requested) R.drawable.ic_usb_on else R.drawable.ic_usb_off
            )
        )
    }

    private companion object {
        const val KEY_REQUESTED_ENABLED = "requested_enabled"
        const val ADB_WIFI_ENABLED = "adb_wifi_enabled"
    }
}
