package name.jayhan.pebble

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.service.notification.Condition
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.service.notification.ZenPolicy
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.RECEIVER_EXPORTED
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

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
fun setupNotifications(
    applicationContext: Context,
    notifications: Notifications
) {
    val notiMan = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    val zenUri = Uri.Builder()
        .scheme(Condition.SCHEME)
        .appendPath("jayhan.name")
        .query("dnd")
        .build()
    val zenPolicy = ZenPolicy.Builder()
        .disallowAllSounds()
        .allowAlarms(true)
        .allowCalls(ZenPolicy.PEOPLE_TYPE_STARRED)
        .showAllVisualEffects()
        .build()
    val zenRule = AutomaticZenRule.Builder("pebble", zenUri)
        .setTriggerDescription("Toggled via Pebble watch")
        .setType(AutomaticZenRule.TYPE_OTHER)
        .setManualInvocationAllowed(true)
        .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        .setEnabled(true)
        .setZenPolicy(zenPolicy)
        .setConfigurationActivity(ComponentName(applicationContext, MainActivity::class.java))
        .build()

    var ruleId = ""
    for (rule in notiMan.automaticZenRules) {
        if (rule.value.name == "pebble") {
            ruleId = rule.key
            break
        }
    }

    if (ruleId == "") {
        try {
            ruleId = notiMan.addAutomaticZenRule(zenRule)
        } catch (e: Exception) {
            println(e)
        }
    } else {
        notiMan.updateAutomaticZenRule(ruleId, zenRule)
        notiMan.setAutomaticZenRuleState(ruleId, Condition(zenUri, "Disabled", Condition.STATE_FALSE))
    }

    val filter = IntentFilter()
    filter.addAction("name.jayhan.pebble.STATUS_BAR_NOTIFICATIONS")
    applicationContext.registerReceiver(notifications, filter, RECEIVER_EXPORTED)
}
