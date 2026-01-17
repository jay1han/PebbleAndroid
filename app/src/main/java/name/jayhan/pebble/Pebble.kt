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
    WATCH_BATT(14),
    WATCH_PLUG(15),
    WATCH_CHG(16)
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
    WBATT(9),
    ACTION(10),
    FRESH(11),
}

enum class ActionType(val code: Int) {
    FIND_PHONE(1),
    DND_TOGGLE(2)
}

class WatchInfo(
    var model: Int = 0,
    var version: Int = 0,
    var battery: Int = 0,
    var plugged: Boolean = false,
    var charging: Boolean = false,
) {
    fun setInfo(
        model: Int,
        version: Int,
    ): WatchInfo {
        return WatchInfo(model, version, battery, plugged, charging)
    }

    fun setBattery(
        battery: Int,
        plugged: Boolean,
        charging: Boolean,
    ): WatchInfo {
        return WatchInfo(model, version, battery, plugged, charging)
    }

    fun versionString(): String {
        return "%d.%d.%d".format(
            (version shr 16) and 0xFF,
            (version shr 8) and 0xFF,
            version and 0xFF
        )
    }

    fun modelString(): String {
        return try {
            WatchModels[model]
        } catch (e: IndexOutOfBoundsException) {
            ""
        }
    }
}

private val Receivers = mapOf(
    com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE to DataReceiver,
    com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE_ACK to AckReceiver,
    com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE_NACK to NackReceiver,
)

private object DataReceiver:
    PebbleKit.PebbleDataReceiver(AppConstants.APP_UUID)
{
    override fun receiveData(context: Context?, transactionId: Int, data: PebbleDictionary?) {
        Pebble.received(true)
        PebbleKit.sendAckToPebble(context, transactionId)

        if (data != null) {
            val msgType = data.getInteger(DictKey.MSG_TYPE.code)?.toInt() ?: 0
            when (msgType) {
                MsgType.INFO.code,
                MsgType.FRESH.code -> {
                    val watchModel = data.getInteger(DictKey.MODEL.code)?.toInt() ?: 0
                    val watchFwVersion = data.getUnsignedIntegerAsLong(DictKey.FW_VERSION.code)?.toInt() ?: 0
                    if (watchModel != 0 && watchFwVersion != 0)
                        Pebble.setWatchInfo(watchModel, watchFwVersion)
                    val tzMinutes = data.getInteger(DictKey.TZ_MIN.code)
                    if (tzMinutes != null)
                        Pebble.fromMinutes(tzMinutes.toInt())
                    if (msgType == MsgType.FRESH.code) {
                        Pebble.doRefresh = true
                    } else {
                        if (context != null)
                            Pebble.sendIntent(context, MsgType.WBATT) {}
                    }
                }

                MsgType.WBATT.code -> {
                    val watchBattery = data.getInteger(DictKey.WATCH_BATT.code)?.toInt() ?: 0
                    val watchPlugged = data.getInteger(DictKey.WATCH_PLUG.code)?.toInt() ?: 0
                    val watchCharging = data.getInteger(DictKey.WATCH_CHG.code)?.toInt() ?: 0
                    Pebble.setBattery(watchBattery, watchPlugged != 0, watchCharging != 0)
                }

                MsgType.ACTION.code -> {
                    // TODO: perform action
                    val action = data.getInteger(DictKey.ACTION.code)?.toInt() ?: 0
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
    var watchInfo = WatchInfo()
    val infoFlow = MutableStateFlow(WatchInfo())
    val isConnected = MutableStateFlow(false)
    private val clock = Clock.System
    val lastReceived = MutableStateFlow("")
    private var lastSent = clock.now()

    fun init(
        context: Context
    ) {
        Log.v(AppConstants.TAG, "Pebble object init")
        Receivers.forEach {
            val filter = IntentFilter(it.key)
            context.registerReceiver(it.value, filter, Context.RECEIVER_EXPORTED)
        }

        sendIntent(context, MsgType.INFO) {}
    }

    // TODO: unregister at onPause
    fun deinit(
        context: Context
    ) {
        Receivers.forEach {
            context.unregisterReceiver(it.value)
        }
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
        watchInfo = watchInfo.setInfo(watchModel, watchFwVersion)
        infoFlow.value = watchInfo
    }

    fun setBattery(
        battery: Int,
        plugged: Boolean,
        charging: Boolean
    ) {
        watchInfo = watchInfo.setBattery(battery, plugged, charging)
        infoFlow.value = watchInfo
        History.event(battery, plugged)
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
        lastReceived.value = clock.now().formatDate()
        doRefresh = isAcked && !isConnected.value
        isConnected.value = isAcked
    }

    var doRefresh = false

    fun askInfo() {
        // TODO: Re-ask WatchInfo
    }

    fun askBattery() {
        // TODO: Re-ask Battery
    }
}
