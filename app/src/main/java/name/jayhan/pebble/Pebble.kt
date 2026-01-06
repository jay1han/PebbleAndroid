package name.jayhan.pebble

import android.content.Context
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE
import com.getpebble.android.kit.PebbleKit
import com.getpebble.android.kit.util.PebbleDictionary
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID
import kotlin.math.absoluteValue

val appUuid: UUID? = UUID.fromString("aaaab139-d4d0-478f-81f4-4cbbe4992461")

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

class PebbleReceiver(
    private val pebble: Pebble,
    ): PebbleKit.PebbleDataReceiver(appUuid) {

    override fun receiveData(context: Context?, transactionId: Int, data: PebbleDictionary?) {
        PebbleKit.sendAckToPebble(context, transactionId)

        if (data != null) {
            val msgType = data.getInteger(DictKey.MSG_TYPE.code)
            when (msgType.toByte()) {
                MsgType.INFO.code -> {
                    val watchModel = data.getInteger(DictKey.MODEL.code).toInt()
                    val watchFwVersion = data.getUnsignedIntegerAsLong(DictKey.FW_VERSION.code).toInt()
                    pebble.setWatchInfo(watchModel, watchFwVersion)
                    val tzMinutes = data.getInteger(DictKey.TZ_MINS.code).toInt()
                    pebble.timezone.fromMinutes(tzMinutes)
                }
            }
        }
    }
}

class Pebble(
    private val applicationContext: Context,
) {
    val timezone = Timezone(this)
    val infoFlow = MutableStateFlow("")
    var isConnected = false

    init {
        val pebbleReceiver = PebbleReceiver(this)
        val dataFilter = IntentFilter(INTENT_APP_RECEIVE)
        ContextCompat.registerReceiver(applicationContext, pebbleReceiver, dataFilter, ContextCompat.RECEIVER_EXPORTED)
    }

    fun askInfo() {
        val initDict = PebbleDictionary()
        initDict.addInt8(DictKey.MSG_TYPE.code, MsgType.INFO.code)
        send(initDict)
    }

    fun send(pebbleDict: PebbleDictionary) {
        PebbleKit.sendDataToPebble(applicationContext, appUuid, pebbleDict)
    }

    fun setWatchInfo(
        watchModel: Int,
        watchFwVersion: Int
    ) {
        isConnected = true
        val versionString = "%d.%d.%d"
            .format(
                (watchFwVersion shr 16) and 0xFF,
                (watchFwVersion shr 8) and 0xFF,
                watchFwVersion and 0xFF
            )
        infoFlow.value = "Model: ${WatchModels[watchModel]}\nVersion: $versionString"
    }
}

class Timezone(
    private val pebble: Pebble
) {
    private var minutes: Int = 0

    val tzFlow = MutableStateFlow("+0.0")

    fun fromString(text: String): String {
        if (text.isEmpty()) return get()
        val negative = text[0] == '-'
        val split = (if (negative) text.substring(1) else text).split('.')

        if (split.size >= 1) {
            if (split[0].isEmpty()) minutes = 0
            else minutes = split[0].toInt() * 60
        }
        if (split.size >= 2) {
            if (!split[1].isEmpty()) {
                val decimal = split[1].toFloat() / 100f
                minutes += (decimal * 60).toInt()
            }
        }
        if (negative) minutes = -minutes

        toPebble()
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

    fun toPebble() {
        val pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.TZ.code)
        pebbleDict.addInt16(DictKey.TZ_MINS.code, minutes.toShort())
        pebble.send(pebbleDict)
    }
}
