package name.jayhan.pebble

import com.getpebble.android.kit.util.PebbleDictionary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.absoluteValue

class Timezone(
    private val pebble: Pebble
) {
    private var minutes: Int = 0

    val tzFlow = MutableStateFlow("+0.0")

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

        toPebble()
        return get()
    }

    fun fromMinutes(tzMinutes: Int) {
        minutes = tzMinutes
        get()
    }

    fun get(): String {
        val sign = if (minutes < 0) "-" else "+"
        val hours = minutes.absoluteValue / 60
        val frac = 100 * (minutes.absoluteValue - hours * 60) / 60
        val string = "$sign${hours}.$frac"
        tzFlow.value = string
        return string
    }

    fun toPebble() {
        val pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.TZ.code)
        pebbleDict.addInt16(DictKey.TZ_MINS.code, minutes.toShort())
        pebble.send(pebbleDict)
    }
}