package name.jayhan.dolbom

val WatchModels = listOf(
    "Unknown",
    "Pebble",
    "Pebble Steel",
    "Pebble Time",
    "Pebble Time Steel",
    "Pebble Time Round 14",
    "Pebble Time Round 20",
    "Pebble 2 HR",
    "Pebble 2 SE",
    "Pebble Time 2",
    "Core 2 Duo",
    "Core Time 2"
)

class WatchInfo(
    var model: Int = 0,
    var version: Int = 0,
    var battery: Int = 0,
    var plugged: Boolean = false,
    var charging: Boolean = false,
) {
    fun setInfo(
        model: Int,
        version: Int,
    ): WatchInfo {
        return WatchInfo(model, version, battery, plugged, charging)
    }

    fun setBattery(
        battery: Int,
        plugged: Boolean,
        charging: Boolean,
    ): WatchInfo {
        return WatchInfo(model, version, battery, plugged, charging)
    }

    fun versionString(): String {
        return "%d.%d.%d".format(
            (version shr 16) and 0xFF,
            (version shr 8) and 0xFF,
            version and 0xFF
        )
    }

    fun modelString(): String {
        return try {
            WatchModels[model]
        } catch (_: IndexOutOfBoundsException) {
            ""
        }
    }

    fun hasInfo(): Boolean {
        return (model != 0 && version != 0)
    }
    
    fun hasBatt(): Boolean {
        return (battery != 0)
    }
}
