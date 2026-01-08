@file:OptIn(ExperimentalMaterial3Api::class)

package name.jayhan.pebble

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

object AppConstants {
    val titleSize = 28.sp
    val textSize = 20.sp
    val smallSize = 16.sp
    val padSize = 8.dp

    val colorBack = Color(0xFFFF8000)
    val colorText = Color(0xFFFFFFFF)

    const val INTENT_SERVICE_STOP = "name.jayhan.pebble.SERVICE_STOP"
    const val INTENT_LISTENER_STOP = "name.jayhan.pebble.LISTENER_STOP"
    const val INTENT_REGISTER_MAP = "name.jayhan.pebble.REGISTER_MAP"
    const val INTENT_RESET_MAP = "name.jayhan.pebble.RESET_MAP"
    const val INTENT_SBN = "name.jayhan.pebble.STATUS_BAR_NOTIFICATIONS"
    const val INTENT_PHONE_INFO = "name.jayhan.pebble.PHONE_INFO"
    const val INTENT_WIFI_INFO = "name.jayhan.pebble.WIFI_INFO"
    const val INTENT_REVIVE = "name.jayhan.pebble.REVIVE_FOREGROUND"

    const val CHANNEL_ID = "PebbleService"

    const val EXTRA_MAP_COUNT = "count"
    const val EXTRA_MAP_KEY = "letter"
    const val EXTRA_MAP_VALUE = "package"
    const val EXTRA_TELE_GEN = "gen"
    const val EXTRA_SSID = "ssid"

    const val PREF_NAME = "name.jayhan.pebble.NOTIFICATIONS_LIST"

    val APP_UUID: UUID? = UUID.fromString("aaaab139-d4d0-478f-81f4-4cbbe4992461")
}

var buildDateTime = ""

class MainActivity : ComponentActivity() {
    private lateinit var context: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val buildDate = Date(BuildConfig.BUILDTIME)
        buildDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(buildDate)

        context = applicationContext
        AppString.init(context)
        getNotificationAccess()

        Mapper.init(context)
        Pebble.init(context)
        Notifications.init(context)

        BatteryReceiver.init(context)
        BluetoothReceiver.init(context)
        WiFiReceiver.init(context)
        PhoneReceiver.init(context)

        val intent = Intent(context, PebbleService::class.java)
        context.startForegroundService(intent)

        Pebble.askInfo()

        setContent {
            Scaffold(
                topBar = {
                    TopBar { stopServices() }
                }
            ) {
                innerPadding ->
                MainPage(
                    Modifier.padding(innerPadding),
                )
            }
        }
    }

    private fun stopServices() {
        val stopForeground = Intent(AppConstants.INTENT_SERVICE_STOP)
        context.sendBroadcast(stopForeground)
        val stopListener = Intent(AppConstants.INTENT_LISTENER_STOP)
        context.sendBroadcast(stopListener)
        finish()
    }

    private fun getNotificationAccess() {
        if (!Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ).contains(context.packageName)) {
            val settingsIntent =
                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            this.startActivity(settingsIntent)
        }
    }
}

object Mapper {
    private lateinit var context: Context

    fun init(
        context: Context
    ) {
        this.context = context
    }

    fun register(
        key: Char,
        value: String
    ) {
        val intent = Intent(AppConstants.INTENT_REGISTER_MAP).apply {
            putExtra(AppConstants.EXTRA_MAP_KEY, key.toString())
            putExtra(AppConstants.EXTRA_MAP_VALUE, value)
        }
        context.sendBroadcast(intent)
    }

    fun reset() {
        val intent = Intent(AppConstants.INTENT_RESET_MAP)
        context.sendBroadcast(intent)
    }
}

object AppString {
    private lateinit var context: Context

    fun init(context: Context) {
        this.context = context
    }

    fun get(id: Int): String {
        return context.getString(id)
    }
}