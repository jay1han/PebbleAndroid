@file:OptIn(ExperimentalMaterial3Api::class)

package name.jayhan.pebble

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

object AppConstants {
    val buildDateTime: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        .format(Date(BuildConfig.BUILDTIME))

    val titleSize = 28.sp
    val textSize = 20.sp
    val smallSize = 16.sp
    val subSize = 12.sp
    val padSize = 8.dp

    val colorBack = Color(0xFFFF8000)
    val colorNotiBack = Color(0xFF000000)
    val colorText = Color(0xFFFFFFFF)
    val colorTop = Color(0xFFFFFFFF)
    val colorBlank = Color(0xFFFFFFFF)
    val colorFade = Color(0xFF808080)

    const val INTENT_SERVICE_STOP = "name.jayhan.pebble.SERVICE_STOP"
    const val INTENT_SBN = "name.jayhan.pebble.STATUS_BAR_NOTIFICATIONS"
    const val INTENT_REVIVE = "name.jayhan.pebble.REVIVE_FOREGROUND"

    const val CHANNEL_ID = "PebbleService"

    const val PREF_NAME = "name.jayhan.pebble.NOTIFICATIONS_LIST"
    const val NOTI_DB = "name.jayhan.pebble.ACTIVE_NOTIFICATIONS"
    const val ACTIVE_COUNT = "count"

    val APP_UUID: UUID? = UUID.fromString("aaaab139-d4d0-478f-81f4-4cbbe4992461")
    val MAX_NOTI_INDICATORS = 15
}

class MainActivity :
    ComponentActivity() {
    private lateinit var context: Context
    private var servicesStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        context = applicationContext

        Permissions.start(context, this) {
            startServices()
        }

        setContent {
            AppScaffold {
                stopServices()
                finish()
            }
        }
    }

    override fun onResume() {
        Permissions.update(NOTIFICATION_LISTENER)
        super.onResume()
    }

    private fun startServices() {
        if (!servicesStarted) {
            val intent = Intent(context, PebbleService::class.java)
            context.startForegroundService(intent)
            servicesStarted = true
        }
    }

    private fun stopServices() {
        if (servicesStarted) {
            val stopForeground = Intent(AppConstants.INTENT_SERVICE_STOP)
            context.sendBroadcast(stopForeground)
        }
    }
}
