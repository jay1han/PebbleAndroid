package name.jayhan.pebble

import android.content.Context
import android.telephony.ServiceState
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import com.getpebble.android.kit.util.PebbleDictionary

class PhoneCallback(
    private val context: Context,
    private val teleMan: TelephonyManager
): TelephonyCallback(), TelephonyCallback.ServiceStateListener {

    fun init() {
        val cellType = teleMan.dataNetworkType
        sendToPebble(getCellGen(cellType))

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
        Pebble.send(pebbleDict)
    }
}
