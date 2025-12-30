package name.jayhan.pebble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.os.BatteryManager
import androidx.annotation.RequiresPermission
import com.getpebble.android.kit.util.PebbleDictionary

class BatteryReceiver(pebble: Pebble): BroadcastReceiver() {
    val pebble = pebble

    override fun onReceive(context: Context, intent: Intent) {
        val isCharging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0)
        val percent = 100.0 * level.toFloat() / scale

        var pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.PHONE_CHG.code)
        pebbleDict.addInt8(DictKey.PHONE_CHG.code, isCharging.toByte())
        pebbleDict.addInt8(DictKey.PHONE_BATT.code, percent.toInt().toByte())
        pebble.send(pebbleDict)
    }
}

class BluetoothReceiver(pebble: Pebble): BroadcastReceiver() {
    private val pebble = pebble

    private fun getBatteryLevel(pairedDevice: BluetoothDevice?): Int {
        val level = pairedDevice?.let { bluetoothDevice ->
            (bluetoothDevice.javaClass.getMethod("getBatteryLevel"))
                .invoke(pairedDevice) as Int
        } ?: 0
        return level
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.STATE_DISCONNECTED)
        if (state != BluetoothAdapter.STATE_CONNECTED) {
            val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            if (device != null) {
                var pebbleDict = PebbleDictionary()
                pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.BT.code)
                // android.permission.BLUETOOTH_CONNECT
                pebbleDict.addString(DictKey.BTID.code, device.name.take(20))
                pebbleDict.addInt8(DictKey.BTC.code, getBatteryLevel(device).toByte())
                pebble.send(pebbleDict)
            }
        }
    }
}

class NetworkCallback(pebble: Pebble): ConnectivityManager.NetworkCallback() {
    private val pebble = pebble

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        var pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.WIFI.code)
        pebbleDict.addString(DictKey.WIFI.code, network.toString())
        pebble.send(pebbleDict)
    }
}
