package name.jayhan.pebble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.telephony.ServiceState
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import androidx.navigationevent.NavigationEventInfo
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
        var pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.BT.code)
        if (state == BluetoothAdapter.STATE_CONNECTED) {
            val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            if (device != null) {
                pebbleDict.addString(DictKey.BTID.code, device.name.take(19))
                pebbleDict.addInt8(DictKey.BTC.code, getBatteryLevel(device).toByte())
            }
        } else {
            // android.permission.BLUETOOTH_CONNECT
            pebbleDict.addString(DictKey.BTID.code, "")
            pebbleDict.addInt8(DictKey.BTC.code, 0)
        }
        pebble.send(pebbleDict)
    }
}

class WiFiCallback(
    connMan: ConnectivityManager,
    pebble: Pebble,
    ): ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
    private val pebble = pebble
    private val connMan = connMan

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun onAvailable(network: Network) {
        super.onAvailable(network)

        val capa = connMan.getNetworkCapabilities(network)
        if (capa != null) sendToPebble(capa)
    }

    override fun onCapabilitiesChanged(
        network: Network,
        networkCapabilities: NetworkCapabilities
    ) {
        super.onCapabilitiesChanged(network, networkCapabilities)
        sendToPebble(networkCapabilities)
    }

    fun sendToPebble(capa: NetworkCapabilities) {
        val info = capa.transportInfo as WifiInfo
        val ssid = info.ssid.removeSurrounding("\"")

        var pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.WIFI.code)
        pebbleDict.addString(DictKey.WIFI.code, ssid.take(19))
        pebble.send(pebbleDict)
    }
}

class MobileCallback(
    teleMan: TelephonyManager,
    pebble: Pebble
): TelephonyCallback(), TelephonyCallback.ServiceStateListener {

    private val teleMan = teleMan
    private val pebble = pebble

    override fun onServiceStateChanged(serviceState: ServiceState) {
        var mobile = 0
        fun bumpTo(to: Int) {
            if (to > mobile) mobile = to
        }

        if (serviceState.state == ServiceState.STATE_IN_SERVICE) {
            for (reginfo in serviceState.networkRegistrationInfoList) {
                when (reginfo.accessNetworkTechnology) {
                    TelephonyManager.NETWORK_TYPE_GSM,
                    TelephonyManager.NETWORK_TYPE_GPRS,
                    TelephonyManager.NETWORK_TYPE_EDGE,
                        -> bumpTo(2)
                    TelephonyManager.NETWORK_TYPE_HSPA,
                    TelephonyManager.NETWORK_TYPE_UMTS,
                        -> bumpTo(3)
                    TelephonyManager.NETWORK_TYPE_LTE
                        -> bumpTo(4)
                    TelephonyManager.NETWORK_TYPE_NR
                        -> bumpTo(5)
                    else -> null
                }
            }

            var pebbleDict = PebbleDictionary()
            pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.NET.code)
            pebbleDict.addInt8(DictKey.NET.code, mobile.toByte())
            pebble.send(pebbleDict)
        }
    }
}