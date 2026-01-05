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
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager.UNKNOWN_SSID
import android.os.BatteryManager
import android.telephony.ServiceState
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.getpebble.android.kit.util.PebbleDictionary

class BatteryReceiver(
    val pebble: Pebble
): BroadcastReceiver() {

    init {
        sendToPebble(0, false)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val isCharging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0)
        val percent = 100.0 * level.toFloat() / scale
        sendToPebble(percent.toInt(), isCharging)
    }

    private fun sendToPebble(
        percent: Int,
        isCharging: Boolean
    ) {
        val pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.PHONE_CHG.code)
        pebbleDict.addInt8(DictKey.PHONE_CHG.code, if (isCharging) 1.toByte() else 0.toByte())
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
        val result = method.invoke(this) as Int
        return result
    } catch (e: Exception) {
        println(e)
        return 0
    }
}

class BluetoothReceiver(
    private val pebble: Pebble,
    blueMan: BluetoothManager
): BroadcastReceiver() {

    init {
        val bluetoothAdapter = blueMan.adapter
        val devices = bluetoothAdapter.bondedDevices
        val connectedDevice = devices.firstOrNull { it.isConnected() }
        val deviceName = connectedDevice?.name ?: ""
        val deviceBattery = connectedDevice?.getBatteryLevel() ?: 0
        sendToPebble(deviceName, deviceBattery)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
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

class WiFiCallback(
    private val pebble: Pebble,
    private val connMan: ConnectivityManager,
    ): ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {

    init {
        sendToPebble("")
    }

    override fun onAvailable(network: Network) {
        super.onAvailable(network)

        val info = connMan.getNetworkCapabilities(network)?.transportInfo
        if (info != null) {
            val wifiInfo = info as WifiInfo
            sendToPebble(wifiInfo.ssid)
        }
    }

    override fun onLost(network: Network) {
        super.onLost(network)

        val info = connMan.getNetworkCapabilities(network)?.transportInfo
        if (info is WifiInfo) {
            sendToPebble("")
        }

    }

    override fun onCapabilitiesChanged(
        network: Network,
        capabilities: NetworkCapabilities
    ) {
        super.onCapabilitiesChanged(network, capabilities)

        val info = capabilities.transportInfo as WifiInfo
        val ssid = info.ssid.removeSurrounding("\"")
        if (ssid == UNKNOWN_SSID) return
        sendToPebble(ssid)
    }

    private fun sendToPebble(ssid: String) {
        val pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.WIFI.code)
        pebbleDict.addString(DictKey.WIFI.code, ssid.take(19))
        pebble.send(pebbleDict)
    }
}

class PhoneCallback(
    private val pebble: Pebble,
    teleMan: TelephonyManager
): TelephonyCallback(), TelephonyCallback.ServiceStateListener {

    init {
        val cellType = teleMan.dataNetworkType
        sendToPebble(getCellGen(cellType))
    }

    private fun sendToPebble(gen: Int) {
        val pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.NET.code)
        pebbleDict.addInt8(DictKey.NET.code, gen.toByte())
        pebble.send(pebbleDict)
    }

    override fun onServiceStateChanged(serviceState: ServiceState) {
        var mobile = 0
        fun bumpTo(to: Int) {
            if (to > mobile) mobile = to
        }

        if (serviceState.state == ServiceState.STATE_IN_SERVICE) {
            for (reginfo in serviceState.networkRegistrationInfoList) {
                bumpTo(getCellGen(reginfo.accessNetworkTechnology))
            }
        } else {
            mobile = 0
        }
        sendToPebble(mobile)
    }

    private fun getCellGen(gen: Int): Int {
        return when (gen) {
            TelephonyManager.NETWORK_TYPE_GSM,
            TelephonyManager.NETWORK_TYPE_GPRS,
            TelephonyManager.NETWORK_TYPE_EDGE,
                -> 2
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_UMTS,
                -> 3
            TelephonyManager.NETWORK_TYPE_LTE
                -> 4
            TelephonyManager.NETWORK_TYPE_NR
                -> 5
            else -> 0
        }
    }
}

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun setupIndicators(
    pebble: Pebble,
    applicationContext: Context
) {
    val batteryReceiver = BatteryReceiver(pebble)
    val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    ContextCompat.registerReceiver(applicationContext, batteryReceiver, batteryFilter, ContextCompat.RECEIVER_EXPORTED)

    val blueMan = applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothReceiver = BluetoothReceiver(pebble, blueMan)
    val bluetoothFilter = IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
    try {
        val intent = ContextCompat.registerReceiver(applicationContext, bluetoothReceiver, bluetoothFilter, ContextCompat.RECEIVER_EXPORTED)
        if (intent != null) {
            println(intent)
        }
    } catch (e: Exception) {
        println(e)
    }

    val connMan = applicationContext.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCallback = WiFiCallback(pebble, connMan)
    val networkRequest = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .build()
    try {
        connMan.registerNetworkCallback(networkRequest, networkCallback)
    } catch (e: Exception) {
        println(e)
    }

    val teleMan = applicationContext.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
    val phoneCallback = PhoneCallback(pebble, teleMan)
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
