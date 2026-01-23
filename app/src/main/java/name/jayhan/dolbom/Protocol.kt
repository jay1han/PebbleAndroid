package name.jayhan.dolbom

import android.content.Context
import android.content.Intent
import android.util.Log
import com.getpebble.android.kit.util.PebbleDictionary
import java.util.UUID

val FACE_UUID: UUID? = UUID.fromString("aaaab139-d4d0-478f-81f4-4cbbe4992461")
val APP_UUID: UUID? = UUID.fromString("6b4862e7-d32d-4f17-a3b8-09aefa729df1")

enum class DictKey {
    ZERO,
    MSG_TYPE,
    MODEL,
    FW_VERSION,
    WATCH_BATT,
    WATCH_PLUG,
    WATCH_CHG,
    TZ_MIN,
    ACTION,
    PHONE_DND,
    PHONE_BATT,
    PHONE_PLUG,
    PHONE_CHG,
    NET,
    SIM,
    CARRIER,
    WIFI,
    BTID,
    BTC,
    BTON,
    NOTI,
}
enum class MsgType {
    ZERO,
    INFO,
    FRESH,
    WBATT,
    ACTION,
    TZ,
    PHONE_DND,
    PHONE_CHG,
    NET,
    WIFI,
    BT,
    NOTI,
}

val MsgName = listOf(
    "NONE", "INFO", "FRESH", "WBATT", "ACTION", "TZ", "PHONE_DND", "PHONE_CHG", "NET", "WIFI", "BT", "NOTI"
)

enum class ActionType {
    ZERO,
    FIND_PHONE,
    DND_TOGGLE,
}

class Protocol
{
    private companion object {
        var sourceDict = mutableMapOf<DictKey, Any?>()
    }
    
    private fun isSameOrUpdate(key: DictKey, value: Any): Boolean {
        if (sourceDict[key] == value) return true
        sourceDict[key] = value
        return false
    }
    
    fun reset() {
        sourceDict = mutableMapOf()
    }
    
    fun send(
        context: Context,
        intent: Intent
    ) {
        val msgType = intent.getIntExtra(Const.EXTRA_MSG_TYPE, 0)
        Log.v(Const.TAG, "out ${MsgName[msgType]}")
        var suppress = false
    
        val pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.ordinal, msgType.toByte())
    
        when(msgType) {
            MsgType.INFO.ordinal,
            MsgType.WBATT.ordinal -> {
            }
    
            MsgType.TZ.ordinal -> {
                val minutes = intent.getIntExtra(Const.EXTRA_TZ_MIN, 0).toShort()
                suppress = isSameOrUpdate(DictKey.TZ_MIN, minutes)
                pebbleDict.addInt16(DictKey.TZ_MIN.ordinal, minutes)
            }
    
            MsgType.PHONE_CHG.ordinal -> {
                val isCharging = intent.getIntExtra(Const.EXTRA_PHONE_CHG, 0).toByte()
                val isPlugged = intent.getIntExtra(Const.EXTRA_PHONE_PLUG, 0).toByte()
                val percent = intent.getIntExtra(Const.EXTRA_PHONE_BATT, 0).toByte()
                suppress = isSameOrUpdate(DictKey.PHONE_CHG, isCharging) &&
                        isSameOrUpdate(DictKey.PHONE_PLUG, isPlugged) &&
                        isSameOrUpdate(DictKey.PHONE_BATT, percent)
                pebbleDict.addInt8(DictKey.PHONE_CHG.ordinal, isCharging)
                pebbleDict.addInt8(DictKey.PHONE_PLUG.ordinal, isPlugged)
                pebbleDict.addInt8(DictKey.PHONE_BATT.ordinal, percent)
            }
    
            MsgType.WIFI.ordinal -> {
                val ssid = (intent.getStringExtra(Const.EXTRA_WIFI) ?: "").take(Const.MAX_LEN_ID)
                suppress = isSameOrUpdate(DictKey.WIFI, ssid)
                pebbleDict.addString(DictKey.WIFI.ordinal, ssid)
            }
    
            MsgType.NET.ordinal -> {
                val gen = intent.getIntExtra(Const.EXTRA_NET, 0).toByte()
                val sim = intent.getIntExtra(Const.EXTRA_SIM, 0).toByte()
                val carrier = (intent.getStringExtra(Const.EXTRA_CARRIER) ?: "").take(Const.MAX_LEN_ID)
                suppress = isSameOrUpdate(DictKey.NET, gen) &&
                        isSameOrUpdate(DictKey.SIM, sim) &&
                        isSameOrUpdate(DictKey.CARRIER, carrier)
                pebbleDict.addInt8(DictKey.NET.ordinal, gen)
                pebbleDict.addInt8(DictKey.SIM.ordinal, sim)
                pebbleDict.addString(DictKey.CARRIER.ordinal, carrier)
            }
    
            MsgType.NOTI.ordinal -> {
                val noti = (intent.getStringExtra(Const.EXTRA_NOTI) ?: "").take(Const.MAX_NOTI_INDICATORS)
                suppress = isSameOrUpdate(DictKey.NOTI, noti)
                pebbleDict.addString(DictKey.NOTI.ordinal, noti)
            }
    
            MsgType.BT.ordinal -> {
                val btid = (intent.getStringExtra(Const.EXTRA_BTID) ?: "").take(Const.MAX_LEN_ID)
                val btc = intent.getIntExtra(Const.EXTRA_BTC, 0).toByte()
                val bton = if (intent.getBooleanExtra(Const.EXTRA_BTON, false)) 1.toByte() else 0.toByte()
                suppress = isSameOrUpdate(DictKey.BTID, btid) &&
                    isSameOrUpdate(DictKey.BTC, btc) &&
                    isSameOrUpdate(DictKey.BTON, bton)
                pebbleDict.addString(DictKey.BTID.ordinal, btid)
                pebbleDict.addInt8(DictKey.BTC.ordinal, btc)
                pebbleDict.addInt8(DictKey.BTON.ordinal, bton)
            }
        }

        if (suppress) Log.v(Const.TAG, "suppressed")
        else Pebble.sendData(context, pebbleDict)
    }
}
