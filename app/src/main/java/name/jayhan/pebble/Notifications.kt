package name.jayhan.pebble

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.getpebble.android.kit.util.PebbleDictionary
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.core.content.edit

class NotificationListener:
    NotificationListenerService() {
    private lateinit var context: Context

    override fun onListenerConnected() {
        context = applicationContext
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

// TODO: Add refresh method

    private fun sendToMain() {
        val active = context.getSharedPreferences(AppConstants.NOTI_DB, MODE_PRIVATE)
        var count = 0
        active.edit {
            clear()
            for (notification in activeNotifications) {
                if (!notification.isOngoing
                    && notification.isClearable
                ) {
                    putString(count.toString(), notification.packageName)
                    count++
                }
            }
            putInt(AppConstants.ACTIVE_COUNT, count)
        }

        sendBroadcast(Intent(AppConstants.INTENT_SBN))
    }
}

private fun emptyMap(): MutableMap<String, Char> {
    return mutableMapOf()
}

object Notifications:
    BroadcastReceiver() {
    private lateinit var packageManager: PackageManager
    private lateinit var packageList: SharedPreferences
    private lateinit var notificationsList: SharedPreferences

    val mapFlow = MutableStateFlow<Map<String, Char>>(emptyMap())
    var activeList = listOf<String>()
    val activeFlow = MutableStateFlow<List<String>>(mutableListOf())
    var allList = listOf<String>()
    val allFlow = MutableStateFlow<List<String>>(mutableListOf())

    private fun readMap() {
        val newMap = emptyMap()
        for (item in packageList.all) {
            val packageName = item.key
            val letterAsString = item.value as String
            if (letterAsString.length == 1)
                newMap[packageName] = letterAsString[0]
        }
        mapFlow.value = newMap
    }

    private fun writeMap(map: MutableMap<String, Char>) {
        packageList.edit {
            clear()
            for (item in map) {
                putString(item.key, item.value.toString())
            }
        }
    }

    fun init(
        context: Context
    ) {
        packageManager = context.packageManager

        packageList = context.getSharedPreferences(
            AppConstants.PREF_NAME,
            Context.MODE_PRIVATE
        )
        notificationsList = context.getSharedPreferences(
            AppConstants.NOTI_DB,
            Context.MODE_PRIVATE)

        readMap()

        val filter = IntentFilter()
            .apply {
                addAction(AppConstants.INTENT_SBN)
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
            }
        context.registerReceiver(this, filter, Context.RECEIVER_EXPORTED)

        updateAllList()
    }

    private fun updateAllList() {
        allList = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
            .map { it.packageName }
            .filter { !it.startsWith("com.android.") }
        allFlow.value = allList
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        
        when (intent.action) {
            AppConstants.INTENT_SBN -> {
                val newList  = mutableListOf<String>()
                val compact: MutableSet<Char> = mutableSetOf()

                val count = notificationsList.getInt(AppConstants.ACTIVE_COUNT, 0)
                for (index in 0..<count) {
                    val name = notificationsList.getString(index.toString(), "")
                    if (name != null) {
                        val letter = mapFlow.value.getOrDefault(name, ' ')
                        if (letter != ' ') compact.add(letter)
                        newList.add(name)
                    }
                }

                activeList = newList
                activeFlow.value = activeList

                val text = compact.joinToString("")
                    .take(AppConstants.MAX_NOTI_INDICATORS)
                val pebbleDict = PebbleDictionary()
                pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.NOTI.code)
                pebbleDict.addString(DictKey.NOTI.code, text)
                Pebble.sendDict(pebbleDict)
            }
        }

        updateAllList()
    }

    fun reset() {
        val newMap = emptyMap()
        writeMap(newMap)
        mapFlow.value = newMap
    }

    fun register(letter: Char, packageName: String) {
        val newMap = mapFlow.value.toMutableMap()
        if (letter == ' ') return
        newMap[packageName] = letter
        writeMap(newMap)
        mapFlow.value = newMap
    }

    fun remove(packageName: String) {
        val newMap = mapFlow.value.toMutableMap()
        newMap.remove(packageName)
        writeMap(newMap)
        mapFlow.value = newMap
    }
}
