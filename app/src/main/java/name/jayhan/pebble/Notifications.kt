package name.jayhan.pebble

class Notifications {
    var letterFromApp: MutableMap<Char, String> = mutableMapOf()

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
}