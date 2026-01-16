package name.jayhan.pebble

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class SingleIndicator(
    var packageName: String = "",
    var channel: String = "",
    var ticker: String = "",
    var letter: Char = ' '
) {
    override fun toString(): String {
        return "$letter\n$packageName\n$channel\n$ticker"
    }

    fun comparator(): String {
        return "$packageName:$channel:$ticker"
    }

    fun equalTo(
        other: SingleIndicator
    ): Boolean {
        return packageName == other.packageName &&
                channel == other.channel &&
                ticker == other.ticker
    }

    companion object {
        fun fromString(
            string: String
        ): SingleIndicator {
            val elements = string.split('\n', limit = 4)
            return SingleIndicator(
                packageName = elements[1],
                channel = elements[2],
                ticker = elements[3],
                letter = elements[0][0],
            )
        }
    }
}

object Indicators
{
    private var allList = mutableListOf<SingleIndicator>()
    val allFlow = MutableStateFlow(mutableListOf<SingleIndicator>())
    private lateinit var savedSettings: SharedPreferences

    fun init(context: Context) {
        savedSettings = context.getSharedPreferences(
            AppConstants.PREF_NAME,
            Context.MODE_PRIVATE
        )

        val newList = mutableListOf<SingleIndicator>()
        savedSettings.all.forEach {
            newList.add(SingleIndicator.fromString(it.key))
        }

        saveList(newList)
    }

    fun getLetter(
        packageName: String,
        channel: String,
        ticker: String
    ): Char {
        var provision = ' '
        var match = 0

        for (indicator in allList) {
            if (indicator.packageName == packageName) {
                if (indicator.channel.isEmpty()) {
                    if (indicator.ticker.isEmpty()) {
                        if (match < 10) {
                            provision = indicator.letter
                            match = 10
                        }
                    } else {
                        if (indicator.ticker == ticker) {
                            if (match < 20) {
                                provision = indicator.letter
                                match = 20
                            }
                        }
                    }
                } else {
                    if (indicator.channel == channel) {
                        if (indicator.ticker.isEmpty()) {
                            if (match < 50) {
                                provision = indicator.letter
                                match = 50
                            }
                        } else {
                            if (indicator.ticker == ticker) {
                                provision = indicator.letter
                                match = 100
                            }
                        }
                    }
                }
            }
        }
        return provision
    }

    fun add(
        indicator: SingleIndicator
    ) {
        allList.add(indicator)
        saveList(allList)
    }

    private fun saveList(
        newList: List<SingleIndicator>
    ) {
        allList = newList.sortedBy { it.comparator() }
            .toMutableList()

        savedSettings.edit {
            clear()
            for (item in allList) {
                putBoolean(item.toString(), true)
            }
            commit()
        }

        allFlow.value = allList
    }

    fun remove(
        indicator: SingleIndicator
    ) {
        val newList = allList.filterNot {
            it.equalTo(indicator)
        }
        saveList(newList)
    }

    fun reset() {
        saveList(listOf())
    }
}

val PreviewIndicators = listOf(
    SingleIndicator("com.android.google.apps.dialer", "", "", 'C'),
    SingleIndicator("com.android.google.apps.messaging", "", "", 'T'),
    SingleIndicator("com.android.google.apps.gm", "jay", "", 'j'),
    SingleIndicator("com.android.google.apps.gm", "pebble","", 'p'),
    SingleIndicator("com.android.google.apps.gm", "", "", 'G'),
    SingleIndicator("com.whatsapp", "", "", 'W'),
    SingleIndicator("com.kakao.talk", "",  "", 'K'),
    SingleIndicator("com.kakao.talk", "", "Bob", 'b')
)
