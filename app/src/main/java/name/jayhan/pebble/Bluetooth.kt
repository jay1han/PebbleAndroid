package name.jayhan.pebble

import android.Manifest
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.annotation.RequiresPermission

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
    
    data class ConnectedDevice(
        val name: String = "",
        val profile: Int = 0,
        var battery: Int = 0,
        var active: Boolean = false,
    )
    var connectedDevices = mutableMapOf<String, ConnectedDevice>()

    init {
        context.registerReceiver(
            this,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            },
            Context.RECEIVER_EXPORTED
        )

        refresh()
    }
    
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onReceive(context: Context, intent: Intent) {
        refresh()
    }

    inner class Listener:
        BluetoothProfile.ServiceListener {

        @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (proxy != null) {
                val startedWith = connectedDevices.size
                connectedDevices = mutableMapOf<String, ConnectedDevice>()
                val devices = proxy.connectedDevices
                devices.forEach {
                    if (it.name.isNotEmpty()) {
                        val name = it.name.clean()
                        val battery = it.getBatteryLevel()
                        val isActive =
                            if (profile == BluetoothProfile.A2DP) (proxy as BluetoothA2dp).isA2dpPlaying(it)
                            else (proxy as BluetoothHeadset).isAudioConnected(it)
                        connectedDevices[name] = ConnectedDevice(name, profile, battery, isActive)
                    }
                }
                if (connectedDevices.size != startedWith)
                    send(connectedDevices)
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            val startedWith = connectedDevices.size
            val deviceList = connectedDevices.keys
            for (device in deviceList) {
                if (connectedDevices[device]!!.profile == profile)
                    connectedDevices.remove(device)
            }
            if (connectedDevices.size != startedWith)
                send(connectedDevices)
        }
    }
    val listener = Listener()

    fun refresh() {
        try {
            blueMan.adapter.getProfileProxy(context, listener, BluetoothProfile.A2DP)
            blueMan.adapter.getProfileProxy(context, listener, BluetoothProfile.HEADSET)
        } catch(e: SecurityException) {
            println(e)
        }
    }

    private fun send(
        connectedDevices: Map<String, ConnectedDevice>
    ) {
        for (device in connectedDevices.values) {
            if (device.active) {
                Pebble.sendIntent(context, MsgType.BT) {
                putExtra(Const.EXTRA_BTID, device.name)
                putExtra(Const.EXTRA_BTC, device.battery)
                }
            }
        }
    }
    private fun String.clean(): String {
        return this.removePrefix("LE-")
    }
}
