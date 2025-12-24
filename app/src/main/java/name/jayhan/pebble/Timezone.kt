package name.jayhan.pebble

class Timezone {
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

    fun toMsg(): String {
        return "$hours.$minutes"
    }
}