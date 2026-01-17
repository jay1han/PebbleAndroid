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

class WifiCallback(
    private val context: Context,
) :
    ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
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

    fun send() {
        Pebble.sendIntent(context, MsgType.WIFI) {
            putExtra(AppConstants.EXTRA_WIFI, ssid)
        }
    }
}

class PhoneCallback(
    private val context: Context,
):
    TelephonyCallback(), TelephonyCallback.ServiceStateListener {

    private val teleMan = context.getSystemService(Context.TELEPHONY_SERVICE)
            as TelephonyManager
    private var mobileGen = 0

    init {
        try {
            val cellType = teleMan.dataNetworkType
            mobileGen = getCellGen(cellType)
            send()

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
        mobileGen = mobile

        send()
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

    fun send() {
        Pebble.sendIntent(context, MsgType.NET) {
            putExtra(AppConstants.EXTRA_NET, mobileGen)
        }
    }
}
