package name.jayhan.pebble

import com.getpebble.android.kit.util.PebbleDictionary
import kotlinx.coroutines.flow.MutableStateFlow

class Timezone(
    pebble: Pebble
) {
    val pebble = pebble
    var minutes: Int = 0

    val tzFlow = MutableStateFlow("")

    fun fromString(text: String): String {
        if (text.isEmpty()) return get()
        val negative = text[0] == '-'
        val split = (if (negative) text.substring(1) else text).split('.')

        if (split.size >= 1) {
            if (split[0].isEmpty()) minutes = 0
            else minutes = split[0].toInt() * 60
        }
        if (split.size >= 2) {
            if (!split[1].isEmpty()) {
                val decimal = split[1].toFloat() / 100f
                minutes += (decimal * 60).toInt()
            }
        }
        if (negative) minutes = -minutes
        tzFlow.value = (minutes.toFloat() / 60f).toString()

        toPebble()
        return get()
    }

    fun fromMinutes(tzMinutes: Int) {
        minutes = tzMinutes
        tzFlow.value = (minutes.toFloat() / 60f).toString()
    }

    fun get(): String {
        return tzFlow.value
    }

    fun toPebble() {
        var pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.TZ.code)
        pebbleDict.addInt16(DictKey.TZ_MINS.code, minutes.toShort())
        pebble.send(pebbleDict)
    }
}