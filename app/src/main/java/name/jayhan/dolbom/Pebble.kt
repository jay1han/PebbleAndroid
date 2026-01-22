package name.jayhan.dolbom

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.getpebble.android.kit.PebbleKit
import com.getpebble.android.kit.util.PebbleDictionary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Clock

private val FaceReceivers = mapOf(
    com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE to FaceDataReceiver,
    com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE_ACK to FaceAckReceiver,
    com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE_NACK to FaceNackReceiver,
)

private object FaceDataReceiver:
    PebbleKit.PebbleDataReceiver(FACE_UUID)
{
    override fun receiveData(context: Context?, transactionId: Int, data: PebbleDictionary?) {
        PebbleKit.sendAckToPebble(context, transactionId)

        if (data != null) {
            val msgType = data.getInteger(DictKey.MSG_TYPE.ordinal)?.toInt() ?: 0
            when (msgType) {
                MsgType.FRESH.ordinal -> {
                    Log.v(Const.TAG, "in Fresh")
                    Pebble.refreshInformation(context)
                }
                
                MsgType.INFO.ordinal -> {
                    val watchModel = data.getInteger(DictKey.MODEL.ordinal)?.toInt() ?: 0
                    val watchFwVersion = data.getUnsignedIntegerAsLong(DictKey.FW_VERSION.ordinal)?.toInt() ?: 0
                    val tzMinutes = data.getInteger(DictKey.TZ_MIN.ordinal)
                    Log.v(Const.TAG, "in INFO $watchModel $watchFwVersion $tzMinutes")
                    
                    if (watchModel != 0 && watchFwVersion != 0)
                        Pebble.setWatchInfo(watchModel, watchFwVersion)
                    if (tzMinutes != null)
                        Timezone.fromMinutes(tzMinutes.toInt())
                    
                    Pebble.updateNotification(context)
                    if (!Pebble.watchInfo.hasBatt())
                        if (context != null) Pebble.sendIntent(context, MsgType.WBATT) {}
                }

                MsgType.WBATT.ordinal -> {
                    val watchBattery = data.getInteger(DictKey.WATCH_BATT.ordinal)?.toInt() ?: 0
                    val watchPlugged = data.getInteger(DictKey.WATCH_PLUG.ordinal)?.toInt() ?: 0
                    val watchCharging = data.getInteger(DictKey.WATCH_CHG.ordinal)?.toInt() ?: 0
                    
                    Log.v(Const.TAG, "in WBAT $watchBattery% plugged $watchPlugged charging $watchCharging")
                    Pebble.setBattery(context, watchBattery, watchPlugged != 0, watchCharging != 0)
                    
                    Pebble.updateNotification(context)
                    if (!Pebble.watchInfo.hasInfo())
                        if (context != null) Pebble.sendIntent(context, MsgType.INFO) {}
                }
            }
            
            Pebble.received(context, true)
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
                            context?.sendBroadcast(Intent(Const.INTENT_FIND))
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

        sendIntent(context, MsgType.WBATT) {}
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
        updateNotification(context)
    }

    fun received(
        context: Context?,
        isAcked: Boolean
    ) {
        isConnected.value = isAcked
        if (isAcked) {
            lastReceived.value = clock.now()
        } else {
            restartService(context)
        }
    }

    fun updateNotification(
        context: Context?
    ) {
        context?.sendBroadcast(Intent(Const.INTENT_UPDATE))
    }
    
    fun restartService(
        context: Context?
    ) {
        context?.sendBroadcast(Intent(Const.INTENT_RESTART))
    }
    
    fun refreshInformation(
        context: Context?
    ) {
        context?.sendBroadcast(Intent(Const.INTENT_REFRESH))
    }
}
