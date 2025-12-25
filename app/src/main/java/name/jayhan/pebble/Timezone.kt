package name.jayhan.pebble

import android.content.Context
import com.getpebble.android.kit.util.PebbleDictionary

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

        toPebble()
        return get()
    }

    fun get(): String {
        val decimal: Float = hours.toFloat() + minutes.toFloat() / 60f
        return decimal.toString()
    }

    fun toPebble() {
        var tz_minutes = hours * 60;
        if (hours < 0) {
            tz_minutes -= minutes;
        } else {
            tz_minutes += minutes;
        }

        var pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.TZ.code)
        pebbleDict.addInt16(DictKey.TZ_MINS.code, tz_minutes.toShort())
        pebble.send(pebbleDict)
    }
}