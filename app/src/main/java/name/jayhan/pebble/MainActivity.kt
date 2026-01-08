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

object Constants {
    val titleSize = 28.sp
    val textSize = 20.sp
    val smallSize = 16.sp
    val padSize = 8.dp

    val colorBack = Color(0xFFFF8000)
    val colorText = Color(0xFFFFFFFF)
}

var buildDateTime = ""

class MainActivity : ComponentActivity() {
    private lateinit var context: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val buildDate = Date(BuildConfig.BUILDTIME)
        buildDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(buildDate)

        context = applicationContext
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
        val stopForeground = Intent("name.jayhan.pebble.SERVICE_STOP")
        context.sendBroadcast(stopForeground)
        val stopListener = Intent("name.jayhan.pebble.LISTENER_STOP")
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
        val intent = Intent("name.jayhan.pebble.REGISTER_MAP").apply {
            putExtra("key", key.toString())
            putExtra("value", value)
        }
        context.sendBroadcast(intent)
    }

    fun reset() {
        val intent = Intent("name.jayhan.pebble.RESET_MAP")
        context.sendBroadcast(intent)
    }
}