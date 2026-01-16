@file:OptIn(ExperimentalMaterial3Api::class)

package name.jayhan.pebble

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import kotlin.time.Instant
import kotlin.time.toJavaInstant

// TODO: Prettify this
fun Instant.formatDate(): String {
    val jInstant = this.toJavaInstant()
    val jdate = Date.from(jInstant)
    return AppConstants.dateFormat.format(jdate)
}

object AppConstants {
    const val TAG = "Capeta"
    const val GITHUB_ANDROID = "https://github.com/jay1han/PebbleAndroid"
    const val GITHUB_PEBBLE = "https://github.com/jay1han/Pebble"
    val dateFormat = SimpleDateFormat("yyyy/MM/dd-HH:mm:ss")
    val buildDateTime = Instant
        .fromEpochMilliseconds(BuildConfig.BUILDTIME)
        .formatDate()

    val titleSize = 28.sp
    val textSize = 20.sp
    val smallSize = 16.sp
    val subSize = 12.sp
    val padSize = 8.dp

    val colorBack = Color(0xFFFF8000)
    val colorNotiBack = Color(0xFF000000)
    val colorText = Color(0xFFFFFFFF)
    val colorBlank = Color(0xFFFFFFFF)
    val colorFade = Color(0xFF808080)
    val colorTransparent = Color(0)
    val colorWarning = Color(0xFFC00000)
    val colorBlack = Color(0xFF000000)

    const val INTENT_REVIVE = "name.jayhan.pebble.REVIVE"

    const val INTENT_SEND_PEBBLE = "name.jayhan.pebble.SEND_PEBBLE"
    const val EXTRA_MSG_TYPE = "msg_type"
    const val EXTRA_PHONE_CHG = "phone_chg"
    const val EXTRA_PHONE_BATT = "phone_batt"
    const val EXTRA_TZ_MIN = "tz_min"
    const val EXTRA_WIFI = "wifi"
    const val EXTRA_NET = "net"
    const val EXTRA_NOTI = "noti"
    const val EXTRA_BTID = "btid"
    const val EXTRA_BTC = "btc"

    const val CHANNEL_ID = "CapetaService"

    const val PREF_NAME = "name.jayhan.pebble.NOTIFICATIONS_LIST"

    val APP_UUID: UUID? = UUID.fromString("aaaab139-d4d0-478f-81f4-4cbbe4992461")
    const val MAX_NOTI_INDICATORS = 15
    const val MAX_LEN_SSID = 19
    const val MAX_LEN_BTID = 19
}

class AppStart:
    BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.v(AppConstants.TAG, "Boot completed")
                val intent = Intent(context, PebbleService::class.java)
                context.startForegroundService(intent)
            }
        }
    }
}

class MainActivity :
    ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.v(AppConstants.TAG, "Start activity")

        val context = applicationContext
        Permissions.initActivity(mainActivity = this)

        val intent = Intent(context, PebbleService::class.java)
        context.startForegroundService(intent)

        setContent {
            // TODO: Show splash until background service started
            AppScaffold(context)
        }
    }

    override fun onResume() {
        val context = applicationContext

        Permissions.update(NOTIFICATION_LISTENER)
        if (Permissions.allGranted) {
            val intent = Intent(context, PebbleService::class.java)
            context.startForegroundService(intent)
        }

        super.onResume()
    }
}
