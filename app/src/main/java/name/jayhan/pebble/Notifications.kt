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
        Notifications.onNotification(activeNotifications)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        Notifications.onNotification(activeNotifications)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        Notifications.onNotification(activeNotifications)
    }
}

private fun emptyMap(): MutableMap<String, Char> {
    return mutableMapOf()
}

object Notifications : BroadcastReceiver()
{
    private lateinit var packageManager: PackageManager
    private lateinit var savedSettings: SharedPreferences

    val indicatorsFlow = MutableStateFlow<Map<String, Char>>(emptyMap())
    val activeFlow = MutableStateFlow<List<String>>(mutableListOf())
    val allFlow = MutableStateFlow<List<String>>(mutableListOf())
    private var mapPackageToName = mapOf<String, String>()
    private var savedNotifications: Array<StatusBarNotification>? = null

    fun onNotification(
        activeNotifications: Array<StatusBarNotification>
    ) {
        if (!this::packageManager.isInitialized) {
            savedNotifications = activeNotifications
            return
        }

        savedNotifications = activeNotifications
        ingest(activeNotifications)
        updateAllList()
    }

    fun ingest(
        activeNotifications: Array<StatusBarNotification>
    ) {
        val compact: MutableSet<Char> = mutableSetOf()
        activeFlow.value = mutableListOf<String>()
            .apply {
                for (packageName in activeNotifications
                    .filter { !it.isOngoing && it.isClearable }
                    .map { it.packageName }
                    .filter { it != "" }
                ) {
                    add(packageName)
                    indicatorsFlow.value[packageName].let {
                        if (it != null) compact.add(it)
                    }
                }
            }

        val text = compact.joinToString("")
            .take(AppConstants.MAX_NOTI_INDICATORS)
        val pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.NOTI.code)
        pebbleDict.addString(DictKey.NOTI.code, text)
        Pebble.sendDict(pebbleDict)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        updateAllList()
    }

    private fun readMap() {
        val newMap = emptyMap()
        for (item in savedSettings.all) {
            val letterAsString = item.value as String
            if (letterAsString.length == 1)
                newMap[item.key] = letterAsString[0]
        }
        indicatorsFlow.value = newMap
    }

    private fun writeMap(map: MutableMap<String, Char>) {
        savedSettings.edit {
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

        savedSettings = context.getSharedPreferences(
            AppConstants.PREF_NAME,
            Context.MODE_PRIVATE
        )

        readMap()

        val filter = IntentFilter()
            .apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
            }
        context.registerReceiver(this, filter, Context.RECEIVER_EXPORTED)

        updateAllList()
    }

    private fun updateAllList() {
        val newList = packageManager
            .queryIntentActivities(
                Intent(Intent.ACTION_MAIN)
                    .apply { addCategory(Intent.CATEGORY_LAUNCHER) },
                0
            )
            .map { it.activityInfo.packageName }

        val newPairs = mutableListOf<Pair<String, String>>()
            .apply {
                for (packageName in newList) {
                    val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    add(Pair(packageName, appName))
                }
            }
            .apply { sortBy { it.second } }

        mapPackageToName = mutableMapOf<String, String>()
            .apply {
                for (pair in newPairs) put(pair.first, pair.second)
            }

        allFlow.value = newPairs.map { it.first }
    }

    fun refresh() {
        if (savedNotifications != null) {
            val activeNotifications = savedNotifications!!
            ingest(activeNotifications)
        }
    }

    fun getAppName(packageName: String): String {
        return mapPackageToName[packageName] ?: ""
    }

    fun reset() {
        val newMap = emptyMap()
        writeMap(newMap)
        indicatorsFlow.value = newMap
    }

    fun register(letter: Char, packageName: String) {
        val newMap = indicatorsFlow.value.toMutableMap()
        if (letter == ' ') return
        newMap[packageName] = letter
        writeMap(newMap)
        indicatorsFlow.value = newMap
    }

    fun remove(packageName: String) {
        val newMap = indicatorsFlow.value.toMutableMap()
        newMap.remove(packageName)
        writeMap(newMap)
        indicatorsFlow.value = newMap
    }
}
