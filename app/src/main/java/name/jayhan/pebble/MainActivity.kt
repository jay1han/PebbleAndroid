@file:OptIn(ExperimentalMaterial3Api::class)

package name.jayhan.pebble

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date

val titleSize = 28.sp
val textSize = 20.sp
val smallSize = 16.sp
val padSize = 8.dp

var buildDateTime = ""

class MainActivity : ComponentActivity() {
    private lateinit var context: Context
    private lateinit var pebble: Pebble
    private lateinit var notifications: Notifications

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val buildDate = Date(BuildConfig.BUILDTIME)
        buildDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(buildDate)

        context = applicationContext
        getNotificationAccess(context)

        pebble = Pebble(context).apply {init()}
        notifications = Notifications(pebble).apply {init(context)}

        BatteryReceiver(pebble).apply {init(context)}
        BluetoothReceiver(pebble).apply {init(context)}
        WiFiReceiver(pebble).apply {init(context)}
        PhoneReceiver(pebble).apply {init(context)}

        val intent = Intent(context, PebbleService::class.java)
        context.startForegroundService(intent)

        pebble.askInfo()
        val colorBlack = Color(0xFFFF8000)

        setContent {
            Scaffold(
                topBar = {
                    Spacer(
                        modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars)
                            .fillMaxWidth()
                            .background(colorBlack)
                    )
                }
            ) {
                innerPadding ->
                MainPage(
                    pebble = pebble,
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
    }

    private fun getNotificationAccess(context: Context) {
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

@Composable
fun MainPage(
    pebble: Pebble,
    modifier: Modifier = Modifier,
    ) {

    Column(
        modifier = modifier,
    ) {
        Watchface(pebble)
        AwayTimezone(pebble.timezone)
        Box(Modifier.height(padSize))
        NotificationsList()
    }
}

@Composable
fun Section(text: String = "") {
    Text(
        text = text,
        fontSize = titleSize,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun Watchface(
    pebble: Pebble
) {
    val watchInfo: String by pebble.infoFlow.collectAsState("")
    val isConnected: Boolean by pebble.isConnected.collectAsState(false)

    Column {
        Section(
            if (isConnected) "Connected"
            else "Disconnected"
        )
        Text(
            text = "App built at $buildDateTime",
            fontSize = smallSize
        )
        if (isConnected) {
            Text(
                text = watchInfo,
                fontSize = textSize,
            )
        } else {
            Button(
                onClick =
                    {
                        pebble.askInfo()
                    },
            ) {
                Text(
                    text = "Reconnect",
                    fontSize = textSize
                )
            }
        }
        Image(
            painter = painterResource(R.drawable.help),
            modifier = Modifier
                .height(200.dp)
                .padding(padSize)
                .fillMaxWidth(),
            contentScale = ContentScale.Fit,
            contentDescription = "Help",
        )
    }
}

@Composable
fun AwayTimezone(
    timezone: Timezone,
) {
    val tzWatch: String by timezone.tzFlow.collectAsState("+0.0")
    var tz by remember { mutableStateOf("+0.0") }
    var editing by remember { mutableStateOf(false) }

    Row (
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Timezone",
            fontSize = titleSize
        )
        if (editing) {
            OutlinedTextField(
                value = tz,
                onValueChange = { tz = it },
                modifier = Modifier.width(100.dp),
                textStyle = TextStyle(fontSize = textSize),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            Button(
                onClick = {
                    editing = false
                    tz = timezone.fromString(tz)
                },
                modifier = Modifier.padding(padSize)
            ) {
                Text(
                    text = "Apply",
                    fontSize = textSize,
                )
            }
        } else {
            tz = tzWatch
            Text(
                text = tzWatch,
                modifier = Modifier.width(100.dp),
                fontSize = textSize,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = {editing = true},
                modifier = Modifier.padding(padSize)
            ) {
                Text(
                    text = "Edit",
                    fontSize = textSize,
                )
            }
        }
    }
}

@Composable
fun NotificationsList() {
    Section("Notifications")

    // TODO: List notifications and edit
}
