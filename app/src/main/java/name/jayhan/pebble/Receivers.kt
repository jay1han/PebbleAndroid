package name.jayhan.pebble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.getpebble.android.kit.util.PebbleDictionary

class BatteryReceiver(
    private val pebble: Pebble,
): BroadcastReceiver() {

    private var isPlugged = false
    private var percent = 0

    fun init(context: Context) {
        sendToPebble()

        val batteryFilter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        context.registerReceiver(this, batteryFilter, RECEIVER_EXPORTED)
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
        sendToPebble()
    }

    private fun sendToPebble() {
        val pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.PHONE_CHG.code)
        pebbleDict.addInt8(DictKey.PHONE_CHG.code, if (isPlugged) 1.toByte() else 0.toByte())
        pebbleDict.addInt8(DictKey.PHONE_BATT.code, percent.toByte())
        pebble.send(pebbleDict)
    }
}

private fun BluetoothDevice.isConnected(): Boolean {
    try {
        val method = this.javaClass.getMethod("isConnected", Int::class.java)
        val result = method.invoke(this, BluetoothDevice.TRANSPORT_BREDR) as Boolean
        return result
    } catch (e: Exception) {
        println(e)
        return false
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
    private val pebble: Pebble,
): BroadcastReceiver() {

    fun init(context: Context) {
        val blueMan = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = blueMan.adapter
        val devices = bluetoothAdapter.bondedDevices
        val connectedDevice = devices.firstOrNull { it.isConnected() }
        if (connectedDevice != null) {
            sendToPebble(connectedDevice.name, connectedDevice.getBatteryLevel())
        } else {
            sendToPebble("", 0)
        }

        try {
            ContextCompat.registerReceiver(
                context,
                this,
                IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED),
                ContextCompat.RECEIVER_EXPORTED
            )
            ContextCompat.registerReceiver(
                context,
                this,
                IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (e: Exception) {
            println(e)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.STATE_DISCONNECTED)
        var name = ""
        var battery = 0

        if (state == BluetoothAdapter.STATE_CONNECTED) {
            val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            if (device != null) {
                name = device.name.take(19)
                battery = device.getBatteryLevel()
            }
        }
        sendToPebble(name, battery)
    }

    private fun sendToPebble(
        name: String,
        battery: Int
    ) {
        val pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.BT.code)
        pebbleDict.addString(DictKey.BTID.code, name.take(19))
        pebbleDict.addInt8(DictKey.BTC.code, battery.toByte())
        pebble.send(pebbleDict)
    }
}
