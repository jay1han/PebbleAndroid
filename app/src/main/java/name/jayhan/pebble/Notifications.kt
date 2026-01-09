package name.jayhan.pebble

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.getpebble.android.kit.util.PebbleDictionary
import kotlinx.coroutines.flow.MutableStateFlow

class NotificationListener:
    NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        sendToMain()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sendToMain()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sendToMain()
    }

    private fun sendToMain() {
        val intent = Intent(AppConstants.INTENT_SBN)
        var index = 0
        for (notification in this.activeNotifications) {
            if (!notification.isOngoing
                && notification.isClearable
            ) {
                intent.putExtra(index.toString(), notification.packageName)
                index++
            }
        }
        intent.putExtra(AppConstants.EXTRA_MAP_COUNT, index)
        sendBroadcast(intent)
    }
}

typealias CharToString = MutableMap<Char, String>
private fun emptyMap(): CharToString {
    return mutableMapOf()
}

object Notifications:
    BroadcastReceiver() {
    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences

    val mapFlow = MutableStateFlow<CharToString>(emptyMap())
    val listFlow = MutableStateFlow<MutableList<String>>(mutableListOf())

    private fun readMap() {
        val newMap = emptyMap()
        for (item in prefs.all) {
            if (item.key.length == 1) {
                val letter = item.key[0]
                val packageName = item.value as String
                newMap[letter] = packageName
            }
        }
        mapFlow.value = newMap
    }

    private fun writeMap(map: CharToString) {
        with (prefs.edit()) {
            clear()
            for (item in map) {
                putString(item.key.toString(), item.value)
            }
            apply()
        }
    }

    fun init(
        context: Context
    ) {
        if (this::context.isInitialized && this.context != context) {
            context.unregisterReceiver(this)
        }

        this.context = context
        prefs = context.getSharedPreferences(
            AppConstants.PREF_NAME,
            Context.MODE_PRIVATE
        )

        readMap()

        val filter = IntentFilter()
            .apply {
                addAction(AppConstants.INTENT_SBN)
                addAction(AppConstants.INTENT_REGISTER_MAP)
                addAction(AppConstants.INTENT_RESET_MAP)
            }
        context.registerReceiver(this, filter, Context.RECEIVER_EXPORTED)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        
        when (intent.action) {
            AppConstants.INTENT_SBN -> {
                val count = intent.getIntExtra(AppConstants.EXTRA_MAP_COUNT, 0)
                val newList: MutableList<String> = mutableListOf()
                val compact: MutableSet<Char> = mutableSetOf()

                for (index in 0..< count) {
                    val name = intent.getStringExtra(index.toString())
                    if (name != null) {
                        val letter = find(name)
                        compact.add(letter)
                        newList.add(name)
                    }
                }
                listFlow.value = newList

                compact.remove(' ')
                val text = compact.joinToString("").take(10)

                val pebbleDict = PebbleDictionary()
                pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.NOTI.code)
                pebbleDict.addString(DictKey.NOTI.code, text)
                Pebble.send(pebbleDict)
            }
            AppConstants.INTENT_REGISTER_MAP -> {
                val key = intent.getStringExtra(AppConstants.EXTRA_MAP_KEY)!![0]
                val value = intent.getStringExtra(AppConstants.EXTRA_MAP_VALUE)!!
                register(key, value)
            }
            AppConstants.INTENT_RESET_MAP -> { resetMap() }
        }
    }

    private fun resetMap() {
        val newMap = emptyMap()
        writeMap(newMap)
        mapFlow.value = newMap
    }

    private fun register(letter: Char, packageName: String) {
        val newMap = emptyMap()

        for (item in mapFlow.value) {
            if (item.key != letter && item.value != packageName) {
                newMap[item.key] = item.value
            }
        }
        if (letter != ' ')
            newMap[letter] = packageName

        writeMap(newMap)
        mapFlow.value = newMap
    }

    private fun find(packageName: String): Char {
        for (item in mapFlow.value) {
            if (item.value == packageName) return item.key
        }
        return ' '
    }
}
