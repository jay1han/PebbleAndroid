package name.jayhan.pebble

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow

fun String.packageName(): String { return this.substringBefore(':') }
fun Map.Entry<String, Char>.packageName(): String { return this.key.packageName() }
fun String.channel(): String { return this.substringAfter(':') }
fun Map.Entry<String, Char>.channel(): String { return this.key.channel() }
fun Map.Entry<String, Char>.letter(): Char { return this.value }
fun Map<String, Char>.addMutableEntry(
    packageName: String,
    channel: String,
    letter: Char
): MutableMap<String, Char> {
    val newMap = this.toMutableMap()
    newMap["$packageName:$channel"] = letter
    return newMap
}
fun sortMap(
    map: MutableMap<String, Char>
): MutableMap<String, Char> {
    val newMap = map.toSortedMap { first, second ->
        val firstAppName = Notifications.getAppName(first.packageName())
        val secondAppName = Notifications.getAppName(second.packageName())
        if (firstAppName == secondAppName)
            first.channel().compareTo(second.channel())
        else
            firstAppName.compareTo(secondAppName)
    }
    return newMap
}

object Indicators
{
    private var allMap = mapOf<String, Char>()
    val allFlow = MutableStateFlow(mapOf<String, Char>())
    private lateinit var savedSettings: SharedPreferences

    fun init(context: Context) {
        savedSettings = context.getSharedPreferences(
            AppConstants.PREF_NAME,
            Context.MODE_PRIVATE
        )

        val newMap: MutableMap<String, Char> =
            try {
                mutableMapOf<String, Char>().apply {
                    savedSettings.all.forEach {
                        put(it.key, (it.value as String)[0])
                    }
                }
            } catch (_: NumberFormatException) {
                mutableMapOf()
            }

        saveMap(newMap)
    }

    fun getLetter(
        packageName: String,
        channel: String
    ): Char {
        var provision = ' '

        for (indicator in allMap) {
            if (indicator.packageName() == packageName) {
                if (indicator.channel() == "")
                    provision = indicator.letter()
                else {
                    if (channel.contains(indicator.channel()))
                        return indicator.letter()
                }
            }
        }
        return provision
    }

    fun add(
        packageName: String,
        channel: String,
        letter: Char
    ) {
        val newList = allMap.addMutableEntry(
            packageName,
            channel,
            letter
            )
        saveMap(newList)
    }

    private fun saveMap(
        newMap: MutableMap<String, Char>
    ) {
        allMap = sortMap(newMap)

        savedSettings.edit {
            clear()
            for (item in allMap) {
                putString(item.key, item.value.toString())
            }
            commit()
        }

        allFlow.value = allMap
    }

    fun remove(
        packageName: String,
        channel: String,
    ) {
        val newMap = allMap.filterNot {
            it.packageName() == packageName && it.channel() == channel
        }.toMutableMap()
        saveMap(newMap)
    }

    fun reset() {
        saveMap(mutableMapOf())
    }
}
