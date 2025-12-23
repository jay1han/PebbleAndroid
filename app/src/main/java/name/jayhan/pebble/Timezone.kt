package name.jayhan.pebble

class Timezone {
    var hours: Int = 0
    var minutes: Int = 0

    fun set(text: String) {
        hours = 0
        minutes = 0
    }

    fun get(): String {
        val decimal: Float = hours.toFloat() + minutes.toFloat() / 60
        return decimal.toString()
    }

    fun toMsg(): String {
        return "$hours.$minutes"
    }
}