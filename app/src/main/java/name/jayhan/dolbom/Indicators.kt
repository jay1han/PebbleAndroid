package name.jayhan.dolbom

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
    var letter: Char = ' ',
    var ignore: Boolean = false,
) {
    fun equalTo(
        other: SingleIndicator
    ): Boolean {
        return packageName == other.packageName &&
                channel == other.channel &&
                filterText == other.filterText &&
                filterType == other.filterType
    }

    companion object {
        fun fromKeyValue(
            key: String,
            value: Boolean
        ): SingleIndicator {
            val elements = key.split('\n', limit = 5)
            return SingleIndicator(
                packageName = elements[1],
                channel = elements[2],
                filterText = elements[3],
                filterType = FilterType.valueOf(elements[4]),
                letter = elements[0][0],
                ignore = value,
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
            Const.PREF_INDIC,
            Context.MODE_PRIVATE
        )

        val newList = mutableListOf<SingleIndicator>()
        savedSettings.all.forEach {
            newList.add(SingleIndicator.fromKeyValue(it.key, it.value as Boolean))
        }
        newList.forEach {
            if (it.ignore) it.letter = ' '
        }

        saveList(newList)
    }

    fun getLetter(
        sbn: StatusBarNotification
    ): Char {
        val packageName = sbn.packageName
        val notification = sbn.notification
        val channel = notification.channelId

        var provision: SingleIndicator? = null
        var match = 0

        for (indicator in allList) {
            if (indicator.packageName == packageName) {
                if (match < 5) {
                    match = 5
                    provision = indicator
                }
                if (indicator.channel.isEmpty()) {
                    if (indicator.filterText.isEmpty()) {
                        if (match < 10) {
                            provision = indicator
                            match = 10
                        }
                    } else {
                        if (notification.matches(indicator)) {
                            if (match < 20) {
                                provision = indicator
                                match = 20
                            }
                        }
                    }
                } else {
                    if (channel.contains(indicator.channel)) {
                        if (indicator.filterText.isEmpty()) {
                            if (match < 50) {
                                provision = indicator
                                match = 50
                            }
                        } else {
                            if (notification.matches(indicator)) {
                                provision = indicator
                                match = 100
                            }
                        }
                    }
                }
            }
        }

        if (match > 0 && provision != null) {
            if (provision.ignore) return ' '
            return provision.letter
        } else return '+'
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
                val key = with (item) {
                    "$letter\n$packageName\n$channel\n$filterText\n${filterType.name}"
                }
                putBoolean(key, item.ignore)
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

enum class FilterType {
    Title { override val r = R.string.filter_title },
    Subtitle { override val r = R.string.filter_sub },
    Info { override val r = R.string.filter_info },
    Text { override val r = R.string.filter_text };
    abstract val r: Int
    
    fun mapExtrasForFilter(
        notification: Notification,
    ): Map<String, String> {
        when (this) {
            Title -> return mapExtrasForList(notification, listOf(
                Notification.EXTRA_TITLE,
                Notification.EXTRA_CONVERSATION_TITLE,
                Notification.EXTRA_TITLE_BIG,
            ))
            
            Subtitle -> return mapExtrasForList(notification, listOf(
                Notification.EXTRA_PEOPLE_LIST,
                Notification.EXTRA_SUB_TEXT,
            ))
            
            Info -> return mapExtrasForList(notification, listOf(
                Notification.EXTRA_SUMMARY_TEXT,
                Notification.EXTRA_INFO_TEXT,
            ))
            
            Text -> return mapExtrasForList(notification, listOf(
                Notification.EXTRA_TEXT,
                Notification.EXTRA_TEXT_LINES,
                Notification.EXTRA_BIG_TEXT,
            ))
        }
    }

    companion object {
        val Strings = FilterType.entries.map { it.r }
        
        fun index(index: Int): FilterType {
            for (filterType in FilterType.entries) {
                if (index == filterType.ordinal) return filterType
            }
            return Info
        }
        
        private fun mapExtrasForList(
            notification: Notification,
            extraList: List<String>
        ): Map<String, String> {
            // Kotlin's very sweet syntactic sugar
            return extraList.associateWith {
                notification.extras.getCharSequence(it)?.toString()
            }.filter { it.value != null } as Map<String, String>
        }
        
        val Extras = mapOf(
            Info to listOf(
                "ticker"
            ),
            Title to listOf(
                Notification.EXTRA_TITLE,
                Notification.EXTRA_CONVERSATION_TITLE,
                Notification.EXTRA_TITLE_BIG,
            ),
            Subtitle to listOf(
                Notification.EXTRA_PEOPLE_LIST,
            ),
            Text to listOf(
                Notification.EXTRA_TEXT,
                Notification.EXTRA_TEXT_LINES,
                Notification.EXTRA_BIG_TEXT,
                Notification.EXTRA_INFO_TEXT,
                Notification.EXTRA_SUB_TEXT,
                Notification.EXTRA_SUMMARY_TEXT,
            ),
        )
    }
}

fun Notification.matches(
    indicator: SingleIndicator
): Boolean {
    return indicator.filterText.isNotEmpty() &&
            indicator.filterType.mapExtrasForFilter(this).any { (key, value) ->
                value.contains(indicator.filterText)
            }
}

val PreviewIndicators = listOf(
    SingleIndicator("com.android.google.apps.dialer", letter = 'C'),
    SingleIndicator("com.android.google.apps.messaging", letter = 'T'),
    SingleIndicator("com.android.google.apps.gm", "jay", letter = 'j'),
    SingleIndicator("com.android.google.apps.gm", "pebble", letter = 'p'),
    SingleIndicator("com.android.google.apps.gm", letter = ' ', ignore = true),
    SingleIndicator("com.whatsapp", letter = 'W'),
    SingleIndicator("com.kakao.talk", letter = ' ', ignore = true),
    SingleIndicator("com.kakao.talk", filterText = "Bob", letter = 'b'),
    SingleIndicator("com.kakao.talk", "talk", "Alice", FilterType.Text, 'b'),
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
