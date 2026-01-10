package name.jayhan.pebble

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.telephony.ServiceState
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import com.getpebble.android.kit.util.PebbleDictionary

class WifiCallback(
    connMan: ConnectivityManager,
) :
    ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
    private lateinit var connMan: ConnectivityManager

    init {
        sendToPebble("")
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
            if (ssid != WifiManager.UNKNOWN_SSID)
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
        if (ssid == WifiManager.UNKNOWN_SSID) return
        sendToPebble(ssid)
    }

    private fun sendToPebble(ssid: String) {
        val pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.WIFI.code)
        pebbleDict.addString(DictKey.WIFI.code, ssid)
        Pebble.sendDict(pebbleDict)
    }
}

class PhoneCallback(
    teleMan: TelephonyManager,
    context: Context
):
    TelephonyCallback(), TelephonyCallback.ServiceStateListener {

    init {
        try {
            val cellType = teleMan.dataNetworkType
            sendToPebble(getCellGen(cellType))

            teleMan.registerTelephonyCallback(
                TelephonyManager.INCLUDE_LOCATION_DATA_FINE,
                context.mainExecutor,
                this
            )
        } catch (e: SecurityException) {
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

    private fun sendToPebble(gen: Int) {
        val pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.NET.code)
        pebbleDict.addInt8(DictKey.NET.code, gen.toByte())
        Pebble.sendDict(pebbleDict)
    }
}
