package name.jayhan.dolbom

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
    private var hasDataConnection = false
    private var activeSim = 0
    private var isRoaming = false
    private var operator = ""

    init {
        scan()

        try {
            teleMan.registerTelephonyCallback(
                TelephonyManager.INCLUDE_LOCATION_DATA_FINE,
                context.mainExecutor,
                this
            )
        } catch (_: SecurityException) {}
    }
    
    override fun onServiceStateChanged(serviceState: ServiceState) {
        scan()
    }

    fun deinit() {
        teleMan.unregisterTelephonyCallback(this)
    }

    private fun scan() {
        try {
            activeSim = 1
            if (teleMan.isMultiSimSupported == TelephonyManager.MULTISIM_ALLOWED) {
                val simMccMnc = teleMan.simOperator
                val subsList = subsMan.activeSubscriptionInfoList
                subsList?.forEach {
                    val simIndex = it.simSlotIndex
                    val mcc = it.mccString
                    val mnc = it.mncString
                    if (simMccMnc == mcc + mnc) {
                        activeSim = simIndex + 1
                    }
                }
            }
            isRoaming = teleMan.isNetworkRoaming

            operator = teleMan.networkOperatorName
            
            hasDataConnection = true
            mobileGen =
                if (teleMan.dataState == TelephonyManager.DATA_CONNECTED)
                    getCellGen(teleMan.dataNetworkType)
                else 0
            if (mobileGen == 0) {
                hasDataConnection = false
                mobileGen = getCellGen(teleMan.voiceNetworkType)
            }
            refresh()

        } catch (_: SecurityException) { }
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

    fun refresh() {
        Pebble.sendIntent(context, MsgType.NET) {
            putExtra(Const.EXTRA_NET, mobileGen or if (hasDataConnection) 0x10 else 0)
            putExtra(Const.EXTRA_SIM, activeSim or if (isRoaming) 0x10 else 0)
            putExtra(Const.EXTRA_CARRIER, operator)
        }
    }
}
