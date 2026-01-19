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

enum class DictKey {
    NONE,
    MSG_TYPE,
    TZ_MIN,
    PHONE_DND,
    PHONE_BATT,
    PHONE_CHG,
    NET,
    WIFI,
    BTID,
    BTC,
    NOTI,
    ACTION,
    MODEL,
    FW_VERSION,
    WATCH_BATT,
    WATCH_PLUG,
    WATCH_CHG,
    SIM,
    CARRIER,
}
enum class MsgType {
    NONE,
    INFO,
    TZ,
    PHONE_DND,
    PHONE_CHG,
    NET,
    WIFI,
    BT,
    NOTI,
    WBATT,
    ACTION,
    FRESH,
}

enum class ActionType {
    NONE,
    FIND_PHONE,
    DND_TOGGLE,
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
        } catch (_: IndexOutOfBoundsException) {
            ""
        }
    }

    fun isValid(): Boolean {
        return (model != 0 && version != 0)
    }
}

private val Receivers = mapOf(
    com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE to DataReceiver,
    com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE_ACK to AckReceiver,
    com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE_NACK to NackReceiver,
)

private object DataReceiver:
    PebbleKit.PebbleDataReceiver(AppConst.APP_UUID)
{
    override fun receiveData(context: Context?, transactionId: Int, data: PebbleDictionary?) {
        Pebble.received(true)
        PebbleKit.sendAckToPebble(context, transactionId)

        if (data != null) {
            val msgType = data.getInteger(DictKey.MSG_TYPE.ordinal)?.toInt() ?: 0
            when (msgType) {
                MsgType.INFO.ordinal,
                MsgType.FRESH.ordinal -> {
                    val watchModel = data.getInteger(DictKey.MODEL.ordinal)?.toInt() ?: 0
                    val watchFwVersion = data.getUnsignedIntegerAsLong(DictKey.FW_VERSION.ordinal)?.toInt() ?: 0
                    if (watchModel != 0 && watchFwVersion != 0)
                        Pebble.setWatchInfo(watchModel, watchFwVersion)
                    val tzMinutes = data.getInteger(DictKey.TZ_MIN.ordinal)
                    if (tzMinutes != null)
                        Pebble.fromMinutes(tzMinutes.toInt())
                    if (msgType == MsgType.FRESH.ordinal) {
                        Pebble.doRefresh = true
                    } else {
                        if (context != null)
                            Pebble.sendIntent(context, MsgType.WBATT) {}
                    }
                }

                MsgType.WBATT.ordinal -> {
                    val watchBattery = data.getInteger(DictKey.WATCH_BATT.ordinal)?.toInt() ?: 0
                    val watchPlugged = data.getInteger(DictKey.WATCH_PLUG.ordinal)?.toInt() ?: 0
                    val watchCharging = data.getInteger(DictKey.WATCH_CHG.ordinal)?.toInt() ?: 0
                    Pebble.setBattery(watchBattery, watchPlugged != 0, watchCharging != 0)
                    if (!Pebble.watchInfo.isValid()) {
                        if (context != null)
                            Pebble.sendIntent(context, MsgType.INFO) {}
                    }
                }

                MsgType.ACTION.ordinal -> {
                    // TODO: perform action
                    val action = data.getInteger(DictKey.ACTION.ordinal)?.toInt() ?: 0
                    when (action) {
                        ActionType.FIND_PHONE.ordinal -> {}
                        ActionType.DND_TOGGLE.ordinal -> {}
                    }
                }
            }
        }
    }
}

private object AckReceiver:
    PebbleKit.PebbleAckReceiver(AppConst.APP_UUID)
{
    override fun receiveAck(context: Context?, transactionId: Int) {
        Pebble.received(true)
    }
}

private object NackReceiver:
    PebbleKit.PebbleNackReceiver(AppConst.APP_UUID)
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
    val lastReceived = MutableStateFlow(Clock.System.now())
    private var lastSent = clock.now()

    fun init(
        context: Context
    ) {
        Log.v(AppConst.TAG, "Pebble object init")
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
        val intent = Intent(AppConst.INTENT_SEND_PEBBLE).apply {
            putExtra(AppConst.EXTRA_MSG_TYPE, msgType.ordinal)
            extra()
        }
        context.sendBroadcast(intent)
    }

    fun sendData(
        context: Context,
        data: PebbleDictionary
    ) {
        PebbleKit.sendDataToPebble(context, AppConst.APP_UUID, data)
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
            putExtra(AppConst.EXTRA_TZ_MIN, minutes)
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

    fun askInfo() {
        // TODO: Re-ask WatchInfo
    }

    fun askBattery() {
        // TODO: Re-ask Battery
    }
}
