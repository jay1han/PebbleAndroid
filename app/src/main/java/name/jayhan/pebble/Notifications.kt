package name.jayhan.pebble

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.getpebble.android.kit.util.PebbleDictionary

class NotificationListener () : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

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

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        this.onNotificationPosted(sbn)
    }
}

class Notifications(
    private val pebble: Pebble
): BroadcastReceiver() {
    private var letterFromApp: MutableMap<Char, String> = mutableMapOf()

    init {
        register('S', "com.google.android.apps.messaging")
        // TODO: Retrieve stored notifications list
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return

        val count = intent.getIntExtra("count", 0)
        val compact: MutableSet<Char> = mutableSetOf()
        for (index in 0..< count) {
            val name = intent.getStringExtra(index.toString())
            if (name != null) {
                val letter = find(name)
                compact.add(letter)
            }
        }
        compact.remove('-')
        val text = compact.joinToString("").take(10)

        val pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.NOTI.code)
        pebbleDict.addString(DictKey.NOTI.code, text)
        pebble.send(pebbleDict)
    }

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
        return '-'
    }

    fun received(packages: MutableList<String>) {
    }
}