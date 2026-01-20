package name.jayhan.pebble

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager

class WifiCallback(
    private val context: Context,
) :
    ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO)
{
    private val connMan = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager
    private var ssid = ""

    init {
        send()
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connMan.registerNetworkCallback(networkRequest, this)
    }
    
    fun deinit() {
        connMan.unregisterNetworkCallback(this)
    }

    override fun onAvailable(network: Network) {
        super.onAvailable(network)

        val info = connMan.getNetworkCapabilities(network)?.transportInfo
        if (info != null) {
            val wifiInfo = info as WifiInfo
            ssid = wifiInfo.ssid
            if (ssid != WifiManager.UNKNOWN_SSID)
                send()
        }
    }

    override fun onLost(network: Network) {
        super.onLost(network)

        val info = connMan.getNetworkCapabilities(network)?.transportInfo
        ssid = ""
        if (info is WifiInfo) {
            send()
        }
    }

    override fun onCapabilitiesChanged(
        network: Network,
        capabilities: NetworkCapabilities
    ) {
        super.onCapabilitiesChanged(network, capabilities)

        val info = capabilities.transportInfo as WifiInfo
        ssid = info.ssid.removeSurrounding("\"")
        if (ssid == WifiManager.UNKNOWN_SSID) return
        send()
    }

    private fun send() {
        Pebble.sendIntent(context, MsgType.WIFI) {
            putExtra(Const.EXTRA_WIFI, ssid)
        }
    }

    fun refresh() {
        send()
    }
}
