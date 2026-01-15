package name.jayhan.pebble

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.getpebble.android.kit.PebbleKit
import com.getpebble.android.kit.util.PebbleDictionary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.absoluteValue
import kotlin.time.Clock

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
    TZ_MIN(2),
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
enum class MsgType(val code: Int) {
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

enum class ActionType(val code: Int) {
    FIND_PHONE(1),
    DND_TOGGLE(2)
}

data class WatchInfo(
    val model: String = "",
    val version: String = ""
)

private object DataReceiver:
    PebbleKit.PebbleDataReceiver(AppConstants.APP_UUID)
{
    override fun receiveData(context: Context?, transactionId: Int, data: PebbleDictionary?) {
        Pebble.received(true)
        PebbleKit.sendAckToPebble(context, transactionId)

        if (data != null) {
            val msgType = data.getInteger(DictKey.MSG_TYPE.code).toInt()
            when (msgType) {
                MsgType.INFO.code -> {
                    val watchModel = data.getInteger(DictKey.MODEL.code).toInt()
                    val watchFwVersion = data.getUnsignedIntegerAsLong(DictKey.FW_VERSION.code).toInt()
                    Pebble.setWatchInfo(watchModel, watchFwVersion)
                    val tzMinutes = data.getInteger(DictKey.TZ_MIN.code).toInt()
                    Pebble.fromMinutes(tzMinutes)
                }

                MsgType.ACTION.code -> {
                    // TODO: perform action
                    val action = data.getInteger(DictKey.ACTION.code).toInt()
                    when (action) {
                        ActionType.FIND_PHONE.code -> {}
                        ActionType.DND_TOGGLE.code -> {}
                    }
                }
            }
        }
    }
}

private object AckReceiver:
    PebbleKit.PebbleAckReceiver(AppConstants.APP_UUID)
{
    override fun receiveAck(context: Context?, transactionId: Int) {
        Pebble.received(true)
    }
}

private object NackReceiver:
    PebbleKit.PebbleNackReceiver(AppConstants.APP_UUID)
{
    override fun receiveNack(context: Context?, transactionId: Int) {
        Pebble.received(false)
    }
}

object Pebble
{
    val infoFlow = MutableStateFlow(WatchInfo())
    val isConnected = MutableStateFlow(false)
    private val clock = Clock.System
    val lastReceived = MutableStateFlow(clock.now())
    private var lastSent = clock.now()

    fun init(
        context: Context
    ) {
        Log.v(AppConstants.TAG, "Pebble object init")
        // TODO: unregister at onPause
        mapOf(
            com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE to DataReceiver,
            com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE_ACK to AckReceiver,
            com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE_NACK to NackReceiver,
        ).forEach {
            val filter = IntentFilter(it.key)
            context.registerReceiver(it.value, filter, Context.RECEIVER_EXPORTED)
        }

        sendIntent(context, MsgType.INFO) {}
    }

    fun sendIntent(
        context: Context,
        msgType: MsgType,
        extra: Intent.() -> Unit
    ) {
        val intent = Intent(AppConstants.INTENT_SEND_PEBBLE).apply {
            putExtra(AppConstants.EXTRA_MSG_TYPE, msgType.code)
            extra()
        }
        context.sendBroadcast(intent)
    }

    fun sendData(
        context: Context,
        data: PebbleDictionary
    ) {
        PebbleKit.sendDataToPebble(context, AppConstants.APP_UUID, data)
        lastSent = clock.now()
    }

    fun setWatchInfo(
        watchModel: Int,
        watchFwVersion: Int
    ) {
        val versionString = "%d.%d.%d"
            .format(
                (watchFwVersion shr 16) and 0xFF,
                (watchFwVersion shr 8) and 0xFF,
                watchFwVersion and 0xFF
            )
        infoFlow.value = WatchInfo(WatchModels[watchModel], versionString)
    }

    private var minutes: Int = 0
    val tzFlow = MutableStateFlow("")

    fun fromString(
        context: Context,
        text: String
    ): String {
        if (text.isEmpty()) return makeString()
        val negative = (text[0] == '-')
        val split = (if (negative) text.substring(1) else text)
            .split('.')

        if (split.isNotEmpty()) {
            minutes =
                try {
                    if (split[0].isNotEmpty()) (split[0].toInt() * 60) else 0
                } catch (_: NumberFormatException) { 0 }
        }
        if (split.size >= 2) {
            if (split[1].isNotEmpty()) {
                try {
                    val decimal = split[1].toFloat() / 100f
                    minutes += (decimal * 60).toInt()
                } catch (_: NumberFormatException) {}
            }
        }
        if (minutes >= 60 * 24) minutes = 0
        if (negative) minutes = -minutes

        sendIntent(context, MsgType.TZ) {
            putExtra(AppConstants.EXTRA_TZ_MIN, minutes)
        }

        return makeString()
    }

    fun fromMinutes(tzMinutes: Int) {
        minutes = tzMinutes
        makeString()
    }

    private fun makeString(): String {
        val sign = if (minutes < 0) "-" else "+"
        val hours = minutes.absoluteValue / 60
        val frac = 100 * (minutes.absoluteValue - hours * 60) / 60
        val string = "$sign${hours}.$frac"
        tzFlow.value = string
        return string
    }

    fun received(isAcked: Boolean) {
        lastReceived.value = clock.now()
        doRefresh = isAcked && !isConnected.value
        isConnected.value = isAcked
    }

    var doRefresh = false
}
