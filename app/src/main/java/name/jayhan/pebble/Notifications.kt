package name.jayhan.pebble

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.getpebble.android.kit.util.PebbleDictionary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationListener:
    NotificationListenerService() {

    private val stopReceiver = StopReceiver()
    private lateinit var context: Context

    override fun onListenerConnected() {
        super.onListenerConnected()

        context = applicationContext
        val filter = IntentFilter().apply { addAction("name.jayhan.pebble.LISTENER_STOP") }
        context.registerReceiver(stopReceiver, filter, RECEIVER_EXPORTED)
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
        val intent = Intent("name.jayhan.pebble.STATUS_BAR_NOTIFICATIONS")
        var index = 0
        for (notification in this.activeNotifications) {
            if (!notification.isOngoing
                && notification.isClearable
            ) {
                intent.putExtra(index.toString(), notification.packageName)
                index++
            }
        }
        intent.putExtra("count", index)
        sendBroadcast(intent)
    }

    inner class StopReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                if (intent.action == "name.jayhan.pebble.LISTENER_STOP") {
                    this@NotificationListener.context.unregisterReceiver(stopReceiver)
                    requestUnbind()
                    stopSelf()
                }
            }
        }
    }
}

typealias CharToString = MutableMap<Char, String>
private fun emptyMap(): CharToString {
    return mutableMapOf()
}

class Notifications(
    private val pebble: Pebble,
    private val context: Context
): BroadcastReceiver() {

    private val prefs = context.getSharedPreferences(
        "name.jayhan.pebble.NOTIFICATIONS_LIST",
        Context.MODE_PRIVATE
    )

    private val _mapFlow = MutableStateFlow<CharToString>(emptyMap())
    val mapFlow = _mapFlow.asStateFlow()

    private fun readMap() {
        val newMap = emptyMap()
        for (item in prefs.all) {
            if (item.key.length == 1) {
                val letter = item.key[0]
                val packageName = item.value as String
                newMap[letter] = packageName
            }
        }
        _mapFlow.value = newMap
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

    fun init() {
        readMap()

        val filter = IntentFilter()
            .apply {
                addAction("name.jayhan.pebble.STATUS_BAR_NOTIFICATIONS")
                addAction("name.jayhan.pebble.REGISTER_MAP")
                addAction("name.jayhan.pebble.RESET_MAP")
            }
        context.registerReceiver(this, filter, Context.RECEIVER_EXPORTED)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        
        when (intent.action) {
            "name.jayhan.pebble.STATUS_BAR_NOTIFICATIONS" -> {
                val count = intent.getIntExtra("count", 0)
                val compact: MutableSet<Char> = mutableSetOf()
                for (index in 0..< count) {
                    val name = intent.getStringExtra(index.toString())
                    if (name != null) {
                        val letter = find(name)
                        compact.add(letter)
                    }
                }
                compact.remove(' ')
                val text = compact.joinToString("").take(10)

                val pebbleDict = PebbleDictionary()
                pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.NOTI.code)
                pebbleDict.addString(DictKey.NOTI.code, text)
                pebble.send(pebbleDict)
            }
            "name.jayhan.pebble.REGISTER_MAP" -> {
                val key = intent.getStringExtra("key")!![0]
                val value = intent.getStringExtra("value")!!
                register(key, value)
            }
            "name.jayhan.pebble.RESET_MAP" -> { resetMap() }
        }
    }

    private fun resetMap() {
        val newMap = emptyMap()
        writeMap(newMap)
        _mapFlow.value = newMap
    }

    private fun register(letter: Char, packageName: String) {
        val newMap = _mapFlow.value
        val key = find(packageName)
        if (key != ' ') {
            newMap.remove(key)
        }
        if (letter != ' ') {
            newMap[letter] = packageName
        }
        writeMap(newMap)
        _mapFlow.value = newMap
    }

    private fun find(packageName: String): Char {
        for (item in _mapFlow.value) {
            if (item.value == packageName) return item.key
        }
        return ' '
    }
}
