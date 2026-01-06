package name.jayhan.pebble

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.getpebble.android.kit.util.PebbleDictionary

class Notifications(
    private val pebble: Pebble
) {
    private var letterFromApp: MutableMap<Char, String> = mutableMapOf()

    inner class NotificationListener(): NotificationListenerService() {
        override fun onNotificationPosted(sbn: StatusBarNotification?) {
            super.onNotificationPosted(sbn)
            sendToPebble(this)
        }

        override fun onNotificationRemoved(sbn: StatusBarNotification?) {
            super.onNotificationRemoved(sbn)
            sendToPebble(this)
        }
    }

    init {
        register('S', "com.google.android.apps.messaging")
        // TODO: Retrieve stored notifications list
    }

    private fun sendToPebble(
        listener: NotificationListenerService
    ) {
        val compact: MutableSet<Char> = mutableSetOf()
        for (notification in listener.activeNotifications) {
            if (!notification.isOngoing
                && notification.isClearable
            ) {
                val letter = find(notification.packageName)
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
}
