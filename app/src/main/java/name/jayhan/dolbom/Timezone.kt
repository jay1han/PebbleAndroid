package name.jayhan.dolbom

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.absoluteValue

object Timezone
{
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
    
        Pebble.sendIntent(context, MsgType.TZ) {
            putExtra(Const.EXTRA_TZ_MIN, minutes)
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
}
