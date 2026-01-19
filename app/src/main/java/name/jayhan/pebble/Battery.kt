package name.jayhan.pebble

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

    init {
        send()

        val batteryFilter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        context.registerReceiver(this, batteryFilter, Context.RECEIVER_EXPORTED)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action) {
            Intent.ACTION_BATTERY_CHANGED -> {
                isPlugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0)
                percent = (100.0 * level.toFloat() / scale).toInt()
            }
            Intent.ACTION_POWER_CONNECTED -> {
                isPlugged = true
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                isPlugged = false
            }
        }
        send()
    }

    private fun send() {
        Pebble.sendIntent(
            context,
            MsgType.PHONE_CHG
        ) {
            putExtra(Const.EXTRA_PHONE_CHG, if (isPlugged) 1 else 0)
            putExtra(Const.EXTRA_PHONE_BATT, percent)
        }
    }
}

