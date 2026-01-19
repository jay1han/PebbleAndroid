package name.jayhan.pebble

import android.content.Context
import android.telephony.ServiceState
import android.telephony.SubscriptionManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager

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
            putExtra(Const.EXTRA_NET, mobileGen)
            putExtra(Const.EXTRA_SIM, activeSim)
            putExtra(Const.EXTRA_CARRIER, operator)
        }
    }

    fun refresh() {
        scan()
    }
}
