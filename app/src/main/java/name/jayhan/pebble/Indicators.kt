package name.jayhan.pebble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.Context.TELEPHONY_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.os.BatteryManager
import android.telephony.ServiceState
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import androidx.compose.ui.unit.IntRect
import androidx.core.content.ContextCompat
import com.getpebble.android.kit.util.PebbleDictionary
import kotlin.run

class BatteryReceiver(
    val pebble: Pebble
): BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val isCharging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0)
        val percent = 100.0 * level.toFloat() / scale

        val pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.PHONE_CHG.code)
        pebbleDict.addInt8(DictKey.PHONE_CHG.code, isCharging.toByte())
        pebbleDict.addInt8(DictKey.PHONE_BATT.code, percent.toInt().toByte())
        pebble.send(pebbleDict)
    }
}

private fun BluetoothDevice.isConnected(): Boolean {
    try {
        val method = this.javaClass.getMethod("isConnected", Int::class.java)
        val result = method.invoke(this, BluetoothDevice.TRANSPORT_BREDR) as Boolean
        return result
    } catch (e: Exception) {
        return false
    }
}

private fun BluetoothDevice.getBatteryLevel(): Int {
    try {
        val method = this.javaClass.getMethod("getBatteryLevel")
        val result = method.invoke(this) as Int
        return result
    } catch (e: Exception) {
        return 0
    }
}

class BluetoothReceiver(
    private val pebble: Pebble
): BroadcastReceiver() {

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.STATE_DISCONNECTED)
        val pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.BT.code)
        if (state == BluetoothAdapter.STATE_CONNECTED) {
            val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            if (device != null) {
                pebbleDict.addString(DictKey.BTID.code, device.name.take(19))
                pebbleDict.addInt8(DictKey.BTC.code, device.getBatteryLevel().toByte())
            }
        } else {
            pebbleDict.addString(DictKey.BTID.code, "")
            pebbleDict.addInt8(DictKey.BTC.code, 0)
        }
        pebble.send(pebbleDict)
    }
}

class WiFiCallback(
    private val connMan: ConnectivityManager,
    private val pebble: Pebble,
    ): ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {

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

        val pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.WIFI.code)
        pebbleDict.addString(DictKey.WIFI.code, ssid.take(19))
        pebble.send(pebbleDict)
    }
}

class PhoneCallback(
    private val teleMan: TelephonyManager,
    private val pebble: Pebble
): TelephonyCallback(), TelephonyCallback.ServiceStateListener {

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
        } else {
            mobile = 0
        }

        val pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.NET.code)
        pebbleDict.addInt8(DictKey.NET.code, mobile.toByte())
        pebble.send(pebbleDict)
    }
}

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun setupIndicators(
    applicationContext: Context,
    pebble: Pebble
) {
    val batteryReceiver = BatteryReceiver(pebble)
    val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    ContextCompat.registerReceiver(applicationContext, batteryReceiver, batteryFilter, ContextCompat.RECEIVER_EXPORTED)

    val bluetoothReceiver = BluetoothReceiver(pebble)
    val blueMan = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter = blueMan.adapter
    val devices = bluetoothAdapter.bondedDevices
    val connectedDevice = devices.firstOrNull { it.isConnected() }
    val deviceName = connectedDevice?.name ?: ""
    val deviceBattery = connectedDevice?.getBatteryLevel() ?: 0

    run {
        val pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.BT.code)
        pebbleDict.addString(DictKey.BTID.code, deviceName)
        pebbleDict.addInt8(DictKey.BTC.code, deviceBattery.toByte())
        pebble.send(pebbleDict)
    }

    val bluetoothFilter = IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
    try {
        val intent = ContextCompat.registerReceiver(applicationContext, bluetoothReceiver, bluetoothFilter, ContextCompat.RECEIVER_EXPORTED)
        if (intent != null) {
            println(intent)
        }
    } catch (e: Exception) {
        println(e)
    }

    // TODO: Read init WiFi SSID
    run {
        val pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.WIFI.code)
        pebbleDict.addString(DictKey.WIFI.code, "")
        pebble.send(pebbleDict)
    }
    val connMan = applicationContext.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCallback = WiFiCallback(connMan, pebble)
    try {
        connMan.registerDefaultNetworkCallback(networkCallback)
    } catch (e: Exception) {
        println(e)
    }

    // TODO: Read init mobile network
    run {
        val pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.NET.code)
        pebbleDict.addInt8(DictKey.NET.code, 0.toByte())
        pebble.send(pebbleDict)
    }
    val teleMan = applicationContext.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
    val phoneCallback = PhoneCallback(teleMan, pebble)
    try {
        teleMan.registerTelephonyCallback(
            TelephonyManager.INCLUDE_LOCATION_DATA_FINE,
            applicationContext.mainExecutor,
            phoneCallback
        )
    } catch (e: Exception) {
        println(e)
    }
}
