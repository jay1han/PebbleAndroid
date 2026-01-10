@file:OptIn(ExperimentalMaterial3Api::class)

package name.jayhan.pebble

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
    val buildDateTime: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        .format(Date(BuildConfig.BUILDTIME))

    val titleSize = 28.sp
    val textSize = 20.sp
    val smallSize = 16.sp
    val padSize = 8.dp

    val colorBack = Color(0xFFFF8000)
    val colorText = Color(0xFFFFFFFF)

    const val INTENT_SERVICE_STOP = "name.jayhan.pebble.SERVICE_STOP"
    const val INTENT_REGISTER_MAP = "name.jayhan.pebble.REGISTER_MAP"
    const val INTENT_RESET_MAP = "name.jayhan.pebble.RESET_MAP"
    const val INTENT_SBN = "name.jayhan.pebble.STATUS_BAR_NOTIFICATIONS"
    const val INTENT_REVIVE = "name.jayhan.pebble.REVIVE_FOREGROUND"

    const val CHANNEL_ID = "PebbleService"

    const val EXTRA_MAP_KEY = "letter"
    const val EXTRA_MAP_VALUE = "package"

    const val PREF_NAME = "name.jayhan.pebble.NOTIFICATIONS_LIST"
    const val NOTI_DB = "name.jayhan.pebble.ACTIVE_NOTIFICATIONS"
    const val ACTIVE_COUNT = "count"

    val APP_UUID: UUID? = UUID.fromString("aaaab139-d4d0-478f-81f4-4cbbe4992461")
}

object AppString {
    private lateinit var context: Context

    fun init(context: Context) {
        if (this::context.isInitialized) return

        this.context = context
    }

    fun get(id: Int): String {
        return context.getString(id)
    }
}

object Mapper {
    private lateinit var context: Context

    fun init(
        context: Context
    ) {
        if (this::context.isInitialized) return

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

class MainActivity :
    ComponentActivity() {
    private lateinit var context: Context
    private var servicesStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        context = applicationContext
        AppString.init(context)
        Mapper.init(context)

        Permissions.start(context, this) {
            startServices()
        }

        setContent {
            AppScaffold() {
                stopServices()
                finish()
            }
        }
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
