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
import java.util.Locale
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.time.toJavaInstant

fun Instant.formatDateTime(): String {
    return SimpleDateFormat("yyyy.MM.dd HH:mm:ss")
        .format(Date.from(this.toJavaInstant()))
}

fun Instant.formatDate(): String {
    return SimpleDateFormat("yyyy/MM/dd")
        .format(Date.from(this.toJavaInstant()))
}

fun Instant.formatTime(): String {
    return SimpleDateFormat("MM/dd HH:mm")
        .format(Date.from(this.toJavaInstant()))
}

fun Instant.formatTimeSecond(): String {
    return SimpleDateFormat("HH:mm:ss")
        .format(Date.from(this.toJavaInstant()))
}

fun Duration.formatDuration(): String {
    var minutes = this.inWholeSeconds / 60

    if (minutes >= 60) {
        var hours = minutes / 60
        minutes %= 60
        if (hours >= 24) {
            val days = hours / 24
            hours %= 24
            return "%d days %d hours".format(days, hours)
        } else return "%d hours %02d minutes".format(hours, minutes)
    } else return "%d minutes".format(minutes)
}

fun Duration.formatDurationSeconds(): String {
    var seconds = this.inWholeSeconds.toInt()
    var minutes = 0
    var hours = 0

    if (seconds >= 60) {
        minutes = seconds / 60
        seconds %= 60
        if (minutes >= 60) {
            hours = minutes / 60
            minutes %= 60
        }
    }
    return "%dh%02dm%02ds".format(hours, minutes, seconds)
}

object AppConstants {
    const val TAG = "Dolbom"
    const val GITHUB_ANDROID = "https://github.com/jay1han/PebbleAndroid"
    const val GITHUB_PEBBLE = "https://github.com/jay1han/Pebble"
    val buildDateTime = Instant
        .fromEpochMilliseconds(BuildConfig.BUILDTIME)
        .formatDateTime()

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

    const val CHANNEL_ID = "Dolbom"

    const val PREF_INDIC = "name.jayhan.pebble.INDICATORS"
    const val PREF_HISTORY = "name.jayhan.pebble.HISTORY"
    const val HIST_DISCHG_RATE = "dischg_rate"
    const val HIST_N_CYCLES = "n_cycles"
    const val HIST_UNPLUG_TIME = "unplug_time"
    const val HIST_PLUG_STATE = "plug_state"
    const val HIST_UNPLUG_LEVEL = "unplug_level"
    const val HIST_INIT_DATE = "init_date"

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
