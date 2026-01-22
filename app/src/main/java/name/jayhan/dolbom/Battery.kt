package name.jayhan.dolbom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class BatteryReceiver(
    private val context: Context
):
    BroadcastReceiver() {
    private var isPlugged = false
    private var percent = 0
    private var isCharging = false

    init {
        refresh()

        val batteryFilter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(BatteryManager.ACTION_CHARGING)
            addAction(BatteryManager.ACTION_DISCHARGING)
        }
        context.registerReceiver(this, batteryFilter, Context.RECEIVER_EXPORTED)
    }
    
    fun deinit() {
        context.unregisterReceiver(this)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action) {
            Intent.ACTION_BATTERY_CHANGED -> {
                isPlugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
                isCharging = intent.getIntExtra(BatteryManager.EXTRA_CHARGING_STATUS, 0) != 0
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0)
                percent = (100.0 * level.toFloat() / scale).toInt()
            }
            Intent.ACTION_POWER_CONNECTED -> isPlugged = true
            Intent.ACTION_POWER_DISCONNECTED -> isPlugged = false
            BatteryManager.ACTION_CHARGING -> isCharging = true
            BatteryManager.ACTION_DISCHARGING -> isCharging = false
        }
        refresh()
    }

    fun refresh() {
        Pebble.sendIntent(context, MsgType.PHONE_CHG) {
            putExtra(Const.EXTRA_PHONE_BATT, percent)
            putExtra(Const.EXTRA_PHONE_CHG, if (isCharging) 1 else 0)
            putExtra(Const.EXTRA_PHONE_PLUG, if (isPlugged) 1 else 0)
        }
    }
}

