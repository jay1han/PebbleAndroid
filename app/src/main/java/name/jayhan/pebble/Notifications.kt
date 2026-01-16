package name.jayhan.pebble

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow

class NotificationListener:
    NotificationListenerService() {
    private lateinit var context: Context

    override fun onListenerConnected() {
        context = applicationContext
        super.onListenerConnected()
        Notifications.onNotification(context, activeNotifications)
    }

    override fun onListenerDisconnected() {
        requestUnbind()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        Notifications.onNotification(context, activeNotifications)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        Notifications.onNotification(context, activeNotifications)
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
        context: Context,
        activeNotifications: Array<StatusBarNotification>
    ) {
        if (!this::packageManager.isInitialized) {
            savedNotifications = activeNotifications
            return
        }

        savedNotifications = activeNotifications
        process(context, activeNotifications)
        updateAllList()
    }

    fun process(
        context: Context,
        activeNotifications: Array<StatusBarNotification>
    ) {
        val compact: MutableSet<Char> = mutableSetOf()
        val activeList = mutableListOf<String>()
            .apply {
                for (notification in activeNotifications
                    .filter { !it.isOngoing && it.isClearable }
                ) {
                    add(notification.packageName)
                    val letter = Indicators.getLetter(
                        notification.packageName,
                        notification.notification.channelId,
                        notification.notification.tickerText?.toString() ?: ""
                    )
                    if (letter != ' ') compact.add(letter)
                }
            }
        activeFlow.value = activeList.dedup()

        val text = compact.joinToString("")
            .take(AppConstants.MAX_NOTI_INDICATORS)

        Pebble.sendIntent(context, MsgType.NOTI) {
            putExtra(AppConstants.EXTRA_NOTI, text)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        updateAllList()
    }

    fun init(
        context: Context
    ) {
        packageManager = context.packageManager

        val filter = IntentFilter()
            .apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
            }
        context.registerReceiver(this, filter, Context.RECEIVER_EXPORTED)

        updateAllList()
        Indicators.init(context)

        if (savedNotifications != null) {
            val activeNotifications = savedNotifications!!
            process(context, activeNotifications)
        }
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

    fun reprocess(
        context: Context
    ) {
        if (savedNotifications != null) {
            val activeNotifications = savedNotifications!!
            process(context, activeNotifications)
        }
    }

    fun getApplicationName(packageName: String): String {
        return mapPackageToName[packageName] ?: ""
    }
}

fun List<String>.dedup(): List<String> {
    val newList = mutableListOf<String>()
    for (item in this) {
        if (!newList.contains(item)) newList.add(item)
    }
    return newList
}
