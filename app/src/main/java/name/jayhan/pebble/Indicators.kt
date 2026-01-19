package name.jayhan.pebble

import android.app.Notification
import android.content.Context
import android.content.SharedPreferences
import android.service.notification.StatusBarNotification
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow

class SingleIndicator(
    var packageName: String = "",
    var channel: String = "",
    var filterText: String = "",
    var filterType: FilterType = FilterType.Title,
    var letter: Char = ' '
) {
    override fun toString(): String {
        return "$letter\n$packageName\n$channel\n$filterText\n${filterType.name}"
    }

    fun equalTo(
        other: SingleIndicator
    ): Boolean {
        return packageName == other.packageName &&
                channel == other.channel &&
                filterText == other.filterText &&
                filterType == other.filterType
    }

    companion object {
        fun fromString(
            string: String
        ): SingleIndicator {
            val elements = string.split('\n', limit = 5)
            val filterType = try {
                if (elements.size > 4) FilterType.valueOf(elements[4])
                else FilterType.Title
            } catch (_: IllegalArgumentException) {
                FilterType.Title
            }
            return SingleIndicator(
                packageName = elements[1],
                channel = elements[2],
                filterText = elements[3],
                filterType = filterType,
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
            AppConst.PREF_INDIC,
            Context.MODE_PRIVATE
        )

        val newList = mutableListOf<SingleIndicator>()
        savedSettings.all.forEach {
            newList.add(SingleIndicator.fromString(it.key))
        }

        saveList(newList)
    }

    fun getLetter(
        sbn: StatusBarNotification
    ): Char {
        val packageName = sbn.packageName
        val notification = sbn.notification
        val channel = notification.channelId

        var provision = '-'
        var match = 0

        for (indicator in allList) {
            if (indicator.packageName == packageName) {
                if (match < 5) {
                    match = 5
                    provision = '+'
                }
                if (indicator.channel.isEmpty()) {
                    if (indicator.filterText.isEmpty()) {
                        if (match < 10) {
                            provision = indicator.letter
                            match = 10
                        }
                    } else {
                        if (notification.matches(indicator)) {
                            if (match < 20) {
                                provision = indicator.letter
                                match = 20
                            }
                        }
                    }
                } else {
                    if (channel.contains(indicator.channel)) {
                        if (indicator.filterText.isEmpty()) {
                            if (match < 50) {
                                provision = indicator.letter
                                match = 50
                            }
                        } else {
                            if (notification.matches(indicator)) {
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
        allList = newList.sortedBy {
            Notifications.getApplicationName(it.packageName) + ":${it.channel}:${it.filterText}"
        }.toMutableList()

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

fun Notification.matches(
    indicator: SingleIndicator
): Boolean {
    return indicator.filterText.isNotEmpty() &&
            this.extras.getCharSequence(FilterTypeExtra[indicator.filterType.ordinal], "")
                .contains(indicator.filterText)
}

val PreviewIndicators = listOf(
    SingleIndicator("com.android.google.apps.dialer", "", "", FilterType.Title,'C'),
    SingleIndicator("com.android.google.apps.messaging", "", "", FilterType.Title,'T'),
    SingleIndicator("com.android.google.apps.gm", "jay", "", FilterType.Title,'j'),
    SingleIndicator("com.android.google.apps.gm", "pebble","", FilterType.Title,'p'),
    SingleIndicator("com.android.google.apps.gm", "", "", FilterType.Title,'G'),
    SingleIndicator("com.whatsapp", "", "", FilterType.Title,'W'),
    SingleIndicator("com.kakao.talk", "",  "", FilterType.Title,'K'),
    SingleIndicator("com.kakao.talk", "", "Bob", FilterType.Title,'b'),
    SingleIndicator("com.kakao.talk", "talk", "Alice", FilterType.Subject,'b'),
)

val PreviewActiveList = listOf(
    "com.android.google.apps.messaging",
    "com.android.google.apps.messaging",
    "com.whatsapp"
)

val PreviewAllList = listOf(
    "com.android.google.apps.messaging",
    "com.android.google.apps.gm",
    "com.whatsapp"
)
