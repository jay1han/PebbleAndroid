package name.jayhan.pebble

import android.app.Application
import android.content.Context
import com.getpebble.android.kit.PebbleKit
import com.getpebble.android.kit.util.PebbleDictionary
import java.util.UUID

val appUuid = UUID.fromString("aaaab139-d4d0-478f-81f4-4cbbe4992461")

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
    ACTION(11)
}
enum class MsgType(val code: Byte) {
    TZ(1),
    PHONE_DND(2),
    PHONE_CHG(3),
    NET(4),
    WIFI(5),
    BT(6),
    NOTI(7),
    ACTION(8)
}

class Pebble(
    applicationContext: Context? = null
) {
    val applicationContext = applicationContext

    fun isConnected(): Boolean {
        if (applicationContext != null) {
            return PebbleKit.isWatchConnected(applicationContext)
        }
        return false
    }

    fun send(pebbleDict: PebbleDictionary) {
        if (applicationContext != null) {
            PebbleKit.sendDataToPebble(applicationContext, appUuid, pebbleDict)
        }
    }
}

class MockPebble {
    fun send(pebbleDict: PebbleDictionary) {
    }
}
