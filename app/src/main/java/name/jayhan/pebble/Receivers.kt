package name.jayhan.pebble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.annotation.RequiresPermission

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
            putExtra(AppConstants.EXTRA_PHONE_CHG, if (isPlugged) 1 else 0)
            putExtra(AppConstants.EXTRA_PHONE_BATT, percent)
        }
    }

    fun refresh() {
        send()
    }
}

private fun BluetoothDevice.getBatteryLevel(): Int {
    try {
        val method = this.javaClass.getMethod("getBatteryLevel")
        var result = method.invoke(this) as Int
        if (result < 0) result = 0
        return result
    } catch (e: Exception) {
        println(e)
        return 0
    }
}

class BluetoothReceiver(
    private val context: Context
):
    BroadcastReceiver() {
    private var blueMan = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    var name = ""
    var battery = 0

    inner class Listener:
        BluetoothProfile.ServiceListener {

        @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (proxy != null) {
                val devices = proxy.connectedDevices
                devices.forEach {
                    if (it.name.isNotEmpty()) {
                        name = it.name
                        battery = it.getBatteryLevel()
                        send()
                        return
                    }
                }
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            name = ""
            battery = 0
            send()
        }
    }
    val listener = Listener()

    fun refresh() {
        try {
            blueMan.adapter.getProfileProxy(context, listener, BluetoothProfile.A2DP)
        } catch(e: SecurityException) {
            println(e)
        }
    }

    init {
        context.registerReceiver(
            this,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            },
            Context.RECEIVER_EXPORTED
        )
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.STATE_DISCONNECTED)
        val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        if (device != null) {
            val deviceName = device.name.clean()
            if (state == BluetoothAdapter.STATE_CONNECTED) {
                name = deviceName
                battery = device.getBatteryLevel()
            } else {
                if (name == deviceName) {
                    name = ""
                    battery = 0
                }
            }
            send()
        }
    }

    private fun send() {
        Pebble.sendIntent(context, MsgType.BT) {
            putExtra(AppConstants.EXTRA_BTID, name.take(AppConstants.MAX_LEN_BTID))
            putExtra(AppConstants.EXTRA_BTC, battery)
        }
    }

    private fun String.clean(): String {
        return this.removePrefix("LE-")
    }
}
