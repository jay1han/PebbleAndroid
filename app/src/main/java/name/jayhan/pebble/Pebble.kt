package name.jayhan.pebble

import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.IntentFilter
import com.getpebble.android.kit.PebbleKit
import com.getpebble.android.kit.util.PebbleDictionary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.absoluteValue

val WatchModels = listOf(
    "Unknown",
    "Pebble",
    "Pebble Steel",
    "Pebble Time",
    "Pebble Time Steel",
    "Pebble Time Round 14",
    "Pebble Time Round 20",
    "Pebble 2 HR",
    "Pebble 2 SE",
    "Pebble Time 2",
    "Core 2 Duo",
    "Core Time 2"
)

enum class DictKey(val code: Int) {
    MSG_TYPE(1),
    TZ_MINS(2),
    PHONE_DND(3),
    PHONE_BATT(4),
    PHONE_CHG(5),
    NET(6),
    WIFI(7),
    BTID(8),
    BTC(9),
    NOTI(10),
    ACTION(11),
    MODEL(12),
    FW_VERSION(13),
}
enum class MsgType(val code: Byte) {
    INFO(1),
    TZ(2),
    PHONE_DND(3),
    PHONE_CHG(4),
    NET(5),
    WIFI(6),
    BT(7),
    NOTI(8),
    ACTION(9)
}

enum class ActionType(val code: Byte) {
    FIND_PHONE(1),
    DND_TOGGLE(2)
}

object PebbleReceiver:
    PebbleKit.PebbleDataReceiver(AppConstants.APP_UUID) {

    override fun receiveData(context: Context?, transactionId: Int, data: PebbleDictionary?) {
        PebbleKit.sendAckToPebble(context, transactionId)

        if (data != null) {
            val msgType = data.getInteger(DictKey.MSG_TYPE.code)
            when (msgType.toByte()) {
                MsgType.INFO.code -> {
                    val watchModel = data.getInteger(DictKey.MODEL.code).toInt()
                    val watchFwVersion = data.getUnsignedIntegerAsLong(DictKey.FW_VERSION.code).toInt()
                    Pebble.setWatchInfo(watchModel, watchFwVersion)
                    val tzMinutes = data.getInteger(DictKey.TZ_MINS.code).toInt()
                    Pebble.fromMinutes(tzMinutes)
                }

                MsgType.ACTION.code -> {
                    // TODO: perform action
                    val action = data.getInteger(DictKey.ACTION.code).toInt()
                    when (action.toByte()) {
                        ActionType.FIND_PHONE.code -> {}
                        ActionType.DND_TOGGLE.code -> {}
                    }
                }
            }
        }
    }
}

class WatchInfo(
    val model: String = "",
    val version: String = ""
)

object Pebble
{
    private lateinit var context: Context
    val infoFlow = MutableStateFlow(WatchInfo())
    val isConnected = MutableStateFlow(false)

    fun init(
        newContext: Context
    ) {
        if (this::context.isInitialized && this.context != newContext) {
            context.unregisterReceiver(PebbleReceiver)
        }
        context = newContext

        val dataFilter = IntentFilter(com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE)
        context.registerReceiver(PebbleReceiver, dataFilter, RECEIVER_EXPORTED)
    }

    fun askInfo() {
        val initDict = PebbleDictionary()
        initDict.addInt8(DictKey.MSG_TYPE.code, MsgType.INFO.code)
        sendDict(initDict)
    }

    fun sendDict(
        pebbleDict: PebbleDictionary
    ) {
        PebbleKit.sendDataToPebble(context, AppConstants.APP_UUID, pebbleDict)
    }

    fun setWatchInfo(
        watchModel: Int,
        watchFwVersion: Int
    ) {
        isConnected.value = true
        val versionString = "%d.%d.%d"
            .format(
                (watchFwVersion shr 16) and 0xFF,
                (watchFwVersion shr 8) and 0xFF,
                watchFwVersion and 0xFF
            )
        infoFlow.value = WatchInfo(WatchModels[watchModel], versionString)
    }

    private var minutes: Int = 0
    val tzFlow = MutableStateFlow("+0.0")

    fun fromString(text: String): String {
        if (text.isEmpty()) return get()
        val negative = (text[0] == '-')
        val split = (if (negative) text.substring(1) else text)
            .split('.')

        if (split.isNotEmpty()) {
            minutes = if (split[0].isNotEmpty()) (split[0].toInt() * 60) else 0
        }
        if (split.size >= 2) {
            if (split[1].isNotEmpty()) {
                val decimal = split[1].toFloat() / 100f
                minutes += (decimal * 60).toInt()
            }
        }
        if (negative) minutes = -minutes

        timezoneToPebble()
        return get()
    }

    fun fromMinutes(tzMinutes: Int) {
        minutes = tzMinutes
        get()
    }

    fun get(): String {
        val sign = if (minutes < 0) "-" else "+"
        val hours = minutes.absoluteValue / 60
        val frac = 100 * (minutes.absoluteValue - hours * 60) / 60
        val string = "$sign${hours}.$frac"
        tzFlow.value = string
        return string
    }

    fun timezoneToPebble() {
        val pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.TZ.code)
        pebbleDict.addInt16(DictKey.TZ_MINS.code, minutes.toShort())
        sendDict(pebbleDict)
    }
}
