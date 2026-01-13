package name.jayhan.pebble

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow

class Indicator(
    val packageName: String,
    val channel: String,
    val letter: Char,
) {
    override fun toString(): String {
        return "$packageName:$letter$channel"
    }
}

object Indicators
{
    private var allList = listOf<Indicator>()
    val allFlow = MutableStateFlow(listOf<Indicator>())
    private lateinit var savedSettings: SharedPreferences

    fun init(context: Context) {
        savedSettings = context.getSharedPreferences(
            AppConstants.PREF_NAME,
            Context.MODE_PRIVATE
        )

        for (item in savedSettings.all) {
            val indicatorString = item.key as String
            if (indicatorString.isNotEmpty())
                add(indicatorString)
        }
    }

    fun getLetter(
        packageName: String,
        channel: String
    ): Char {
        for (indicator in allList) {
            if (indicator.packageName == packageName &&
                (indicator.channel != "" && channel.contains(indicator.channel)))
                return indicator.letter
        }
        return ' '
    }

    fun hasPackage(
        packageName: String
    ): Boolean {
        for (indicator in allList) {
            if (indicator.packageName == packageName)
                return true
        }
        return false
    }

    fun add(
        packageName: String,
        channel: String,
        letter: Char
    ) {
        val newList = allList.toMutableList().apply {
            add(Indicator(packageName, channel, letter))
        }
        reflow(newList)
    }

    private fun reflow(
        newList: MutableList<Indicator>
    ) {
        newList.sortWith { first, second ->
            val firstAppName = Notifications.getAppName(first.packageName)
            val secondAppName = Notifications.getAppName(second.packageName)
            if (firstAppName == secondAppName)
                first.channel.compareTo(second.channel)
            else
                firstAppName.compareTo(secondAppName)
        }

        savedSettings.edit {
            clear()
            for (item in newList) {
                putBoolean(item.toString(), true)
            }
            commit()
        }

        allList = newList
        allFlow.value = newList
    }

    fun add(
        fromString: String
    ) {
        val packageName = fromString.substringBefore(':')
        val letter = fromString.substringAfter(':')[0]
        val channel = fromString.substringAfter(':').substring(1)
        val newList = allList.toMutableList().apply {
            add(Indicator(packageName, channel, letter))
        }
        reflow(newList)
    }

    fun remove(
        packageName: String,
        channel: String,
    ) {
        val newList = allList.filterNot {
            it.packageName == packageName && it.channel == channel
        }.toMutableList()
        reflow(newList)
    }

    fun reset() {
        reflow(mutableListOf())
    }
}
