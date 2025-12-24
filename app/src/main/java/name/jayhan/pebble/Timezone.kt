package name.jayhan.pebble

import android.content.Context
import com.getpebble.android.kit.util.PebbleDictionary

const val KeyTimezoneHour = 1
const val KeyTimezoneMin = 2

class Timezone(
    pebble: Pebble
) {
    val pebble = pebble
    var hours: Int = 0
    var minutes: Int = 0

    fun set(text: String): String {
        val split = text.split('.')

        if (split.size >= 1) {
            if (split[0].isEmpty()) hours = 0
            else hours = split[0].toInt()
        }
        if (split.size >= 2) {
            if (split[1].isEmpty()) minutes = 0
            else {
                val decimal = split[1].toFloat() / 100f
                minutes = (decimal * 60).toInt()
            }
        }

        return get()
    }

    fun get(): String {
        val decimal: Float = hours.toFloat() + minutes.toFloat() / 60f
        return decimal.toString()
    }

    fun toPebble() {
        var pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(KeyMsgType, MsgType.TIMEZONE.code)
        pebbleDict.addInt8(KeyTimezoneHour, hours.toByte())
        pebbleDict.addInt8(KeyTimezoneMin, minutes.toByte())
        pebble.send(pebbleDict)
    }
}