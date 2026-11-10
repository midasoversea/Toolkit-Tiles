package com.wstxda.toolkit.tiles.usbdebugging

import android.service.quicksettings.Tile
import com.wstxda.toolkit.activity.WriteSecureSettingsActivity
import com.wstxda.toolkit.base.BaseTileService
import com.wstxda.toolkit.manager.usbdebugging.UsbDebuggingModule
import com.wstxda.toolkit.ui.icon.UsbDebuggingIconProvider
import com.wstxda.toolkit.ui.label.UsbDebuggingLabelProvider
import kotlinx.coroutines.flow.Flow

class UsbDebuggingTileService : BaseTileService() {

    private val adbControlManager by lazy { UsbDebuggingModule.getInstance(applicationContext) }
    private val labelProvider by lazy { UsbDebuggingLabelProvider(applicationContext) }
    private val iconProvider by lazy { UsbDebuggingIconProvider(applicationContext) }

    override fun onClick() {
        if (!adbControlManager.hasPermission()) {
            startActivityAndCollapse(WriteSecureSettingsActivity::class.java)
            return
        }
        adbControlManager.toggle()
        updateTile()
    }

    override fun flowsToCollect(): List<Flow<*>> = listOf(
        adbControlManager.isEnabled,
    )

    override fun updateTile() {
        val hasPermission = adbControlManager.hasPermission()
        val isEnabled = adbControlManager.isEnabled.value

        setTileState(
            state = if (hasPermission && isEnabled) {
                Tile.STATE_ACTIVE
            } else {
                Tile.STATE_INACTIVE
            },
            label = labelProvider.getLabel(),
            subtitle = labelProvider.getSubtitle(isEnabled, hasPermission),
            icon = iconProvider.getIcon(isEnabled),
        )
    }
}
