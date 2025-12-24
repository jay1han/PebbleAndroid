package name.jayhan.pebble

import android.app.Application
import android.content.Context
import com.getpebble.android.kit.PebbleKit
import com.getpebble.android.kit.util.PebbleDictionary
import java.util.UUID

val appUuid = UUID.fromString("aaaab139-d4d0-478f-81f4-4cbbe4992461")

const val KeyMsgType = 0
enum class MsgType(val code: Byte) {
    TEST(0),
    TIMEZONE(1),
    QUIET(2),
    BATTERY(3),
    WIFI(4),
    BT(5),
    NET(6),
    NOTI(7)
}

class Pebble(
    applicationContext: Context? = null
) {
    val applicationContext = applicationContext

    fun isConnected(): Boolean {
        return false
        if (applicationContext != null)
            return PebbleKit.isWatchConnected(applicationContext)
        return false
    }

    fun send(pebbleDict: PebbleDictionary) {
        return
        if (applicationContext != null) {
            pebbleDict.addInt32(KeyTimezoneHour, 0)
            pebbleDict.addInt32(KeyTimezoneMin, 0)
            PebbleKit.sendDataToPebble(applicationContext, appUuid, pebbleDict)
        }
    }
}

class MockPebble {
    fun send(pebbleDict: PebbleDictionary) {
    }
}
