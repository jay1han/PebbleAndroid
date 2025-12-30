package name.jayhan.pebble

import android.content.Context
import com.getpebble.android.kit.PebbleKit
import com.getpebble.android.kit.util.PebbleDictionary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

val appUuid = UUID.fromString("aaaab139-d4d0-478f-81f4-4cbbe4992461")

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

class DataReceiver(
    pebble: Pebble,
    timezone: Timezone
    ): PebbleKit.PebbleDataReceiver(appUuid) {
    private val pebble = pebble
    private val timezone = timezone

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
                    timezone.fromMinutes(tzMinutes)
                }
            }
        }
    }
}

class Pebble(
    applicationContext: Context? = null,
) {
    private val applicationContext = applicationContext
    var isConnected = false

    val infoFlow = MutableStateFlow("")

    fun askInfo() {
        val initDict = PebbleDictionary()
        initDict.addInt8(DictKey.MSG_TYPE.code, MsgType.INFO.code)
        send(initDict)
    }

    fun send(pebbleDict: PebbleDictionary) {
        if (applicationContext != null) {
            PebbleKit.sendDataToPebble(applicationContext, appUuid, pebbleDict)
        }
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
