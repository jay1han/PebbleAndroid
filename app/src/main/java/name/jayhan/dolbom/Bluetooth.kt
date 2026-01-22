package name.jayhan.dolbom

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

class BluetoothReceiver(
    private val context: Context,
) : BroadcastReceiver() {
    private var blueMan = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val proxy = BluetoothProxy()
    
    init {
        try {
            blueMan.adapter.getProfileProxy(context, proxy, BluetoothProfile.A2DP)
            blueMan.adapter.getProfileProxy(context, proxy, BluetoothProfile.HEADSET)
        } catch (_: SecurityException) {
        }
        
        context.registerReceiver(
            this,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)
                addAction(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED)
            },
            Context.RECEIVER_EXPORTED
        )
    }
    
    fun deinit() {
        context.unregisterReceiver(this)
    }
    
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onReceive(context: Context, intent: Intent) {
        proxy.refresh()
    }
    
    data class ConnectedDevice(
        val name: String = "",
        var battery: Int = 0,
        var active: Boolean = false,
    ) {
        fun isValid() = name.isNotEmpty() && battery > 0
        fun isSameAs(other: ConnectedDevice) =
                name == other.name &&
                active == other.active &&
                battery == other.battery
    }

    var lastDevice = ConnectedDevice(battery = 200)
    private fun send(
        device: ConnectedDevice,
    ) {
        if (!lastDevice.isSameAs(device))
            lastDevice = device
        refresh()
    }

    fun refresh() {
        Pebble.sendIntent(context, MsgType.BT) {
            putExtra(Const.EXTRA_BTID, lastDevice.name)
            putExtra(Const.EXTRA_BTC, lastDevice.battery)
            putExtra(Const.EXTRA_BTON, lastDevice.active)
        }
    }
    
    inner class BluetoothProxy :
        BluetoothProfile.ServiceListener {
        var a2dpProxy: BluetoothA2dp? = null
        var headsetProxy: BluetoothHeadset? = null
        
        @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.A2DP) a2dpProxy = proxy as BluetoothA2dp
            else headsetProxy = proxy as BluetoothHeadset
            refresh()
        }
        
        @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.A2DP) a2dpProxy = null
            else headsetProxy = null
            refresh()
        }
        
        @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
        fun refresh() {
            var headsetDevice = ConnectedDevice()
            headsetProxy?.connectedDevices?.forEach {
                headsetDevice = ConnectedDevice(
                    it.name.cleanLE(),
                    it.getBatteryLevel(),
                    headsetProxy?.isAudioConnected(it) ?: false
                )
            }
            
            var a2dpDevice = ConnectedDevice()
            a2dpProxy?.connectedDevices?.forEach {
                a2dpDevice = ConnectedDevice(
                    it.name.cleanLE(),
                    it.getBatteryLevel(),
                    a2dpProxy?.isA2dpPlaying(it) ?: false
                )
            }

            // In order of priority
            send(
                device = when {
                    headsetDevice.active -> headsetDevice
                    a2dpDevice.active -> a2dpDevice
                    headsetDevice.isValid() -> headsetDevice
                    a2dpDevice.isValid() -> a2dpDevice
                    headsetDevice.name.isNotEmpty() -> headsetDevice
                    a2dpDevice.name.isNotEmpty() -> a2dpDevice
                    else -> ConnectedDevice()
                }
            )
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
    
    private fun String.cleanLE(): String {
        return this.removePrefix("LE-")
    }
}
