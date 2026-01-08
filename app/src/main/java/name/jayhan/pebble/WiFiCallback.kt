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
import android.net.wifi.WifiManager
import com.getpebble.android.kit.util.PebbleDictionary

class WiFiCallback(
    private val context: Context,
    private val connMan: ConnectivityManager,
): ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {

    fun init() {
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
            if (ssid != WifiManager.UNKNOWN_SSID) send(wifiInfo.ssid)
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
        if (ssid == WifiManager.UNKNOWN_SSID) return
        send(ssid)
    }

    private fun send(ssid: String) {
        val intent = Intent("name.jayhan.pebble.WIFI_INFO")
            .putExtra("ssid", ssid)
        context.sendBroadcast(intent)
    }
}

object WiFiReceiver:
    BroadcastReceiver() {

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
        Pebble.send(pebbleDict)
    }
}
