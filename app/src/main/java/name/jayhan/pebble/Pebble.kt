package name.jayhan.pebble

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.getpebble.android.kit.PebbleKit
import com.getpebble.android.kit.util.PebbleDictionary
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID
import kotlin.time.Clock

val FACE_UUID: UUID? = UUID.fromString("aaaab139-d4d0-478f-81f4-4cbbe4992461")
val APP_UUID: UUID? = UUID.fromString("6b4862e7-d32d-4f17-a3b8-09aefa729df1")

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
    NONE,
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

private val FaceReceivers = mapOf(
    com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE to FaceDataReceiver,
    com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE_ACK to FaceAckReceiver,
    com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE_NACK to FaceNackReceiver,
)

private object FaceDataReceiver:
    PebbleKit.PebbleDataReceiver(FACE_UUID)
{
    override fun receiveData(context: Context?, transactionId: Int, data: PebbleDictionary?) {
        Pebble.received(context, true)
        PebbleKit.sendAckToPebble(context, transactionId)

        if (data != null) {
            val msgType = data.getInteger(DictKey.MSG_TYPE.ordinal)?.toInt() ?: 0
            when (msgType) {
                MsgType.FRESH.ordinal -> {
                    Pebble.restartService(context)
                }
                
                MsgType.INFO.ordinal -> {
                    val watchModel = data.getInteger(DictKey.MODEL.ordinal)?.toInt() ?: 0
                    val watchFwVersion = data.getUnsignedIntegerAsLong(DictKey.FW_VERSION.ordinal)?.toInt() ?: 0
                    if (watchModel != 0 && watchFwVersion != 0)
                        Pebble.setWatchInfo(watchModel, watchFwVersion)
                    val tzMinutes = data.getInteger(DictKey.TZ_MIN.ordinal)
                    if (tzMinutes != null)
                        Timezone.fromMinutes(tzMinutes.toInt())
                    if (context != null)
                        Pebble.sendIntent(context, MsgType.WBATT) {}
                }

                MsgType.WBATT.ordinal -> {
                    val watchBattery = data.getInteger(DictKey.WATCH_BATT.ordinal)?.toInt() ?: 0
                    val watchPlugged = data.getInteger(DictKey.WATCH_PLUG.ordinal)?.toInt() ?: 0
                    val watchCharging = data.getInteger(DictKey.WATCH_CHG.ordinal)?.toInt() ?: 0
                    Pebble.setBattery(context, watchBattery, watchPlugged != 0, watchCharging != 0)
                    if (!Pebble.watchInfo.isValid())
                        Pebble.restartService(context)
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

private object FaceAckReceiver:
    PebbleKit.PebbleAckReceiver(FACE_UUID)
{
    override fun receiveAck(context: Context?, transactionId: Int) {
        Pebble.received(context, true)
    }
}

private object FaceNackReceiver:
    PebbleKit.PebbleNackReceiver(FACE_UUID)
{
    override fun receiveNack(context: Context?, transactionId: Int) {
        Pebble.received(context, false)
    }
}

private val AppReceivers = mapOf(
    com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE to AppDataReceiver,
    com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE_ACK to AppAckReceiver,
    com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE_NACK to AppNackReceiver,
)

private object AppDataReceiver:
    PebbleKit.PebbleDataReceiver(APP_UUID)
{
    override fun receiveData(context: Context?, transactionId: Int, data: PebbleDictionary?) {
        Pebble.received(context, true)
        PebbleKit.sendAckToPebble(context, transactionId)

        if (data != null) {
            val msgType = data.getInteger(DictKey.MSG_TYPE.ordinal)?.toInt() ?: 0
            when (msgType) {
                MsgType.ACTION.ordinal -> {
                    // TODO: perform action
                    val action = data.getInteger(DictKey.ACTION.ordinal)?.toInt() ?: 0
                    when (action) {
                        ActionType.FIND_PHONE.ordinal -> {
                        }
                        ActionType.DND_TOGGLE.ordinal -> {
                        }
                    }
                }
            }
        }
    }
}

private object AppAckReceiver:
    PebbleKit.PebbleAckReceiver(APP_UUID)
{
    override fun receiveAck(context: Context?, transactionId: Int) {
        Pebble.received(context, true)
    }
}

private object AppNackReceiver:
    PebbleKit.PebbleNackReceiver(APP_UUID)
{
    override fun receiveNack(context: Context?, transactionId: Int) {
        Pebble.received(context, false)
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
        Log.v(Const.TAG, "Pebble object init")
        FaceReceivers.forEach {
            val filter = IntentFilter(it.key)
            context.registerReceiver(it.value, filter, Context.RECEIVER_EXPORTED)
        }
        AppReceivers.forEach {
            val filter = IntentFilter(it.key)
            context.registerReceiver(it.value, filter, Context.RECEIVER_EXPORTED)
        }

        sendIntent(context, MsgType.INFO) {}
    }

    // TODO: unregister at onPause
    fun deinit(
        context: Context
    ) {
        FaceReceivers.forEach {
            context.unregisterReceiver(it.value)
        }
        AppReceivers.forEach {
            context.unregisterReceiver(it.value)
        }
    }

    fun sendIntent(
        context: Context,
        msgType: MsgType,
        extra: Intent.() -> Unit
    ) {
        val intent = Intent(Const.INTENT_SEND_PEBBLE).apply {
            putExtra(Const.EXTRA_MSG_TYPE, msgType.ordinal)
            extra()
        }
        context.sendBroadcast(intent)
    }

    fun sendData(
        context: Context,
        data: PebbleDictionary
    ) {
        PebbleKit.sendDataToPebble(context, FACE_UUID, data)
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
        context: Context?,
        battery: Int,
        plugged: Boolean,
        charging: Boolean
    ) {
        watchInfo = watchInfo.setBattery(battery, plugged, charging)
        infoFlow.value = watchInfo
        History.event(battery, plugged)
        updateService(context)
    }

    fun received(
        context: Context?,
        isAcked: Boolean
    ) {
        if (isAcked) {
            lastReceived.value = clock.now()
        } else {
            updateService(context)
        }
        isConnected.value = isAcked
    }

    fun updateService(
        context: Context?
    ) {
        if (context != null) {
            context.sendBroadcast(Intent(Const.INTENT_UPDATE))
        }
    }
    
    fun restartService(
        context: Context?
    ) {
        if (context != null) {
            context.sendBroadcast(Intent(Const.INTENT_RESTART))
        }
    }
}
