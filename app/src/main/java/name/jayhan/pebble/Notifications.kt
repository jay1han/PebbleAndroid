package name.jayhan.pebble

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.getpebble.android.kit.util.PebbleDictionary
import kotlinx.coroutines.flow.MutableStateFlow

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

object Notifications : BroadcastReceiver()
{
    private lateinit var packageManager: PackageManager

    val activeFlow = MutableStateFlow<List<String>>(mutableListOf())
    val allFlow = MutableStateFlow<List<String>>(mutableListOf())
    private var savedNotifications: Array<StatusBarNotification>? = null
    private var mapPackageToName = mapOf<String, String>()

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
                for (notification in activeNotifications
                    .filter { !it.isOngoing && it.isClearable }
                ) {
                    add(notification.packageName)
                    val letter = Indicators.getLetter(
                        notification.packageName,
                        notification.notification.channelId
                    )
                    if (letter != ' ') compact.add(letter)
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

    fun init(
        context: Context
    ) {
        packageManager = context.packageManager
        Indicators.init(context)

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
}
