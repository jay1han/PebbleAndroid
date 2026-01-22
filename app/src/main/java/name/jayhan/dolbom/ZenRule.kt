package name.jayhan.dolbom

import android.app.Activity
import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.net.Uri
import android.os.Bundle
import android.service.notification.Condition
import android.service.notification.ZenPolicy

object ZenRuleActivity: Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}

class ZenRule(
    context: Context
) {
    init {
        val notiMan = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
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
            .setConfigurationActivity(ComponentName(context, ZenRuleActivity::class.java))
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
    }
}
