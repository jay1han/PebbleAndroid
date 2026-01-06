package name.jayhan.pebble

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager.UNKNOWN_SSID
import android.telephony.ServiceState
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import com.getpebble.android.kit.util.PebbleDictionary

class WiFiCallback(
    private val context: Context,
    private val connMan: ConnectivityManager,
    ): ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {

    init {
        send("")
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connMan.registerNetworkCallback(networkRequest, this)
    }

    override fun onAvailable(network: Network) {
        super.onAvailable(network)

        val info = connMan.getNetworkCapabilities(network)?.transportInfo
        if (info != null) {
            val wifiInfo = info as WifiInfo
            val ssid = wifiInfo.ssid
            if (ssid != UNKNOWN_SSID) send(wifiInfo.ssid)
        }
    }

    override fun onLost(network: Network) {
        super.onLost(network)

        val info = connMan.getNetworkCapabilities(network)?.transportInfo
        if (info is WifiInfo) {
            send("")
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
        send(ssid)
    }

    private fun send(ssid: String) {
        val intent = Intent("name.jayhan.pebble.WIFI_INFO")
            .putExtra("ssid", ssid)
        context.sendBroadcast(intent)
    }
}

class WiFiReceiver(
    private val pebble: Pebble
): BroadcastReceiver() {

    fun init(context: Context) {
        val filter = IntentFilter("name.jayhan.pebble.WIFI_INFO")
        context.registerReceiver(this, filter, Context.RECEIVER_EXPORTED)
        sendToPebble("")
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null) {
            val ssid = intent.getStringExtra("ssid")
            sendToPebble(ssid ?: "")
        }
    }

    private fun sendToPebble(ssid: String) {
        val pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.WIFI.code)
        pebbleDict.addString(DictKey.WIFI.code, ssid)
        pebble.send(pebbleDict)
    }
}

class PhoneCallback(
    private val context: Context,
    teleMan: TelephonyManager
): TelephonyCallback(), TelephonyCallback.ServiceStateListener {

    init {
        val cellType = teleMan.dataNetworkType
        send(getCellGen(cellType))

        try {
            teleMan.registerTelephonyCallback(
                TelephonyManager.INCLUDE_LOCATION_DATA_FINE,
                context.mainExecutor,
                this
            )
        } catch (e: Exception) {
            println(e)
        }
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
        send(mobile)
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

    private fun send(gen: Int) {
        val intent = Intent("name.jayhan.pebble.PHONE_INFO")
            .putExtra("gen", gen)
        context.sendBroadcast(intent)
    }
}

class PhoneReceiver(
    private val pebble: Pebble
): BroadcastReceiver() {

    fun init(context: Context) {
        val filter = IntentFilter("name.jayhan.pebble.PHONE_INFO")
        context.registerReceiver(this, filter, Context.RECEIVER_EXPORTED)
        sendToPebble(0)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null) {
            val gen = intent.getIntExtra("gen", 0)
            sendToPebble(gen)
        }
    }

    private fun sendToPebble(gen: Int) {
        val pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.NET.code)
        pebbleDict.addInt8(DictKey.NET.code, gen.toByte())
        pebble.send(pebbleDict)
    }
}
