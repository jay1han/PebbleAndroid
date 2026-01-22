@file:OptIn(ExperimentalMaterial3Api::class)

package name.jayhan.dolbom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import name.jayhan.dolbom.ui.theme.PebbleTheme
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.time.toJavaInstant

class AppStart:
    BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.v(Const.TAG, "Boot completed")
                if (!Permissions.allGranted) {
                    val intent = Intent(context, PebbleService::class.java)
                    context.startForegroundService(intent)
                }
            }
        }
    }
}

class MainActivity :
    ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.v(Const.TAG, "Start activity")

        val context = applicationContext
        Permissions.initWithActivity(
            mainActivity = this,
            context = context,
            onAllGranted =  {
                val intent = Intent(context, PebbleService::class.java)
                context.startForegroundService(intent)
            },
        )

        enableEdgeToEdge()
        setContent {
            PebbleTheme {
                AppScaffold(context)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        Permissions.updateAll()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Permissions.quitActivity(mainActivity = this)
    }
}

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

fun Duration.formatDurationMinutes(): String {
    var minutes = this.inWholeSeconds / 60

    if (minutes >= 60) {
        var hours = minutes / 60
        minutes %= 60
        if (hours >= 24) {
            val days = hours / 24
            hours %= 24
            if (days > 30) return "%dd".format(days)
            else return "%dd%dh".format(days, hours)
        } else return "%dh%02dm".format(hours, minutes)
    } else return "%dm".format(minutes)
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
            if (hours >= 24) {
                val days = hours / 24
                hours %= 24
                if (days > 30) return "%dd".format(days)
                else return "%dd%dh".format(days, hours)
            } else return "%dh%02dm".format(hours, minutes)
        } else return "%dm%02ds".format(minutes, seconds)
    } else return "%ds".format(seconds)
}

fun Drawable.toImageBitmap(): ImageBitmap {
    val bitmap = createBitmap(this.intrinsicWidth, this.intrinsicHeight)
    val canvas = Canvas(bitmap)
    this.setBounds(0, 0, canvas.width, canvas.height)
    this.draw(canvas)
    return bitmap.asImageBitmap()
}

fun getApplicationIcon(
    context: Context,
    packageName: String
): ImageBitmap? {
    if (packageName.isEmpty()) return null

    val drawable = try {
        context.packageManager.getApplicationIcon(packageName)
    } catch (_: PackageManager.NameNotFoundException) {
        return null
    }

    return drawable.toImageBitmap()
}
