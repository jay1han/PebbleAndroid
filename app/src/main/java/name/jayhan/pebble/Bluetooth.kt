package name.jayhan.pebble

class Bluetooth {
    var mnemo: MutableMap<String, String> = mutableMapOf()

    fun register(device: String, name: String) {
        mnemo[device] = name
    }

    fun deregister(device: String) {
        mnemo.remove(device)
    }

    fun find(device: String): String {
        return mnemo[device] ?: "BT"
    }

    fun toMsg(device: String, battery: Int): String {
        return find(device) + ":$battery"
    }

    fun toMsg(device: String): String {
        return find(device)
    }
}