package name.jayhan.pebble

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.ServiceState
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission

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
            putExtra(AppConst.EXTRA_WIFI, ssid)
        }
    }

    fun refresh() {
        send()
    }
}

class PhoneCallback(
    private val context: Context,
):
    TelephonyCallback(), TelephonyCallback.ServiceStateListener {

    private val teleMan = context.getSystemService(Context.TELEPHONY_SERVICE)
            as TelephonyManager
    private val subsMan = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
            as SubscriptionManager
    private var mobileGen = 0
    private var activeSim = 0
    private var operator = ""

    private fun scan() {
        try {
            val isRoaming = teleMan.isNetworkRoaming
            if (isRoaming) activeSim = activeSim or 0x10

            if (teleMan.isMultiSimSupported == TelephonyManager.MULTISIM_ALLOWED) {
                activeSim = activeSim and 0x10
                val simMccMnc = teleMan.simOperator
                val subsList = subsMan.activeSubscriptionInfoList
                subsList?.forEach {
                    val simIndex = it.simSlotIndex
                    val mcc = it.mccString
                    val mnc = it.mncString
                    if (simMccMnc == mcc + mnc) {
                        activeSim = activeSim or (simIndex + 1)
                    }
                }
            } else {
                activeSim = activeSim and 0x10
            }

            operator = teleMan.networkOperatorName
            val isDataConnected = teleMan.dataState
            if (isDataConnected == TelephonyManager.DATA_CONNECTED) {
                val cellType = teleMan.dataNetworkType
                mobileGen = getCellGen(cellType)
            } else mobileGen = 0
            send()

        } catch (_: SecurityException) { }
    }

    init {
        scan()

        try {
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
        scan()
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

    private fun send() {
        Pebble.sendIntent(context, MsgType.NET) {
            putExtra(AppConst.EXTRA_NET, mobileGen)
            putExtra(AppConst.EXTRA_SIM, activeSim)
            putExtra(AppConst.EXTRA_CARRIER, operator)
        }
    }

    fun refresh() {
        scan()
    }
}
