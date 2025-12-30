package name.jayhan.pebble

import com.getpebble.android.kit.util.PebbleDictionary


class Notifications(
    pebble: Pebble
) {
    private val pebble = pebble
    private var letterFromApp: MutableMap<Char, String> = mutableMapOf()

    fun register(letter: Char, app: String) {
        letterFromApp[letter] = app
    }

    fun deregister(letter: Char) {
        letterFromApp.remove(letter)
    }

    fun find(app: String): Char {
        for (item in letterFromApp) {
            if (item.value == app) return item.key
        }
        return '+'
    }

    fun received(packages: MutableList<String>) {
        var compact: MutableList<Char> = mutableListOf()
        for (name in packages) {
            val letter = 'a'
            compact.add(letter)
        }
        val text = compact.joinToString("").take(10)

        var pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.NOTI.code)
        pebbleDict.addString(DictKey.NOTI.code, text)
        pebble.send(pebbleDict)
    }
}