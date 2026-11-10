package com.wstxda.toolkit.ui.label

import android.content.Context
import com.wstxda.toolkit.R

class UsbDebuggingLabelProvider(private val context: Context) {

    fun getLabel(): CharSequence {
        return context.getString(R.string.adb_control_tile)
    }

    fun getSubtitle(isRequestedEnabled: Boolean, hasPermission: Boolean): CharSequence {
        if (!hasPermission) {
            return context.getString(R.string.tile_setup)
        }

        return if (isRequestedEnabled) {
            context.getString(R.string.adb_control_requested_on)
        } else {
            context.getString(R.string.adb_control_requested_off)
        }
    }
}
