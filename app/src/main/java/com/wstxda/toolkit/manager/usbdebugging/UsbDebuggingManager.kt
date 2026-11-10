package com.wstxda.toolkit.manager.usbdebugging

import android.content.Context
import android.provider.Settings
import com.wstxda.toolkit.permissions.PermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Controls USB and Wireless debugging together.
 *
 * A third-party app cannot reliably read ADB_ENABLED on Android, so the tile
 * persists the last requested state instead of presenting it as verified
 * system state.
 */
class UsbDebuggingManager(context: Context) {

    private val appContext = context.applicationContext
    private val contentResolver = appContext.contentResolver
    private val permissionManager = PermissionManager(appContext)
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _isEnabled = MutableStateFlow(
        preferences.getBoolean(KEY_REQUESTED_STATE, false)
    )
    val isEnabled = _isEnabled.asStateFlow()

    fun hasPermission(): Boolean = permissionManager.hasWriteSecureSettingsPermission()

    @Synchronized
    fun toggle(): Boolean {
        if (!hasPermission()) return false
        return applyState(!_isEnabled.value)
    }

    @Synchronized
    private fun applyState(enabled: Boolean): Boolean {
        val value = if (enabled) 1 else 0
        return try {
            val usbWritten = Settings.Global.putInt(
                contentResolver, Settings.Global.ADB_ENABLED, value
            )
            val wirelessWritten = Settings.Global.putInt(
                contentResolver, KEY_ADB_WIFI_ENABLED, value
            )

            if (usbWritten && wirelessWritten) {
                preferences.edit().putBoolean(KEY_REQUESTED_STATE, enabled).apply()
                _isEnabled.value = enabled
                true
            } else {
                rollbackToPreviousState()
                false
            }
        } catch (_: Exception) {
            rollbackToPreviousState()
            false
        }
    }

    private fun rollbackToPreviousState() {
        val previousValue = if (_isEnabled.value) 1 else 0
        runCatching {
            Settings.Global.putInt(
                contentResolver, Settings.Global.ADB_ENABLED, previousValue
            )
            Settings.Global.putInt(
                contentResolver, KEY_ADB_WIFI_ENABLED, previousValue
            )
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "adb_control_preferences"
        const val KEY_REQUESTED_STATE = "requested_enabled"
        const val KEY_ADB_WIFI_ENABLED = "adb_wifi_enabled"
    }
}
