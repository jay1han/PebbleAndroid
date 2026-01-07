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
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date

val titleSize = 28.sp
val textSize = 20.sp
val smallSize = 16.sp
val padSize = 8.dp

val colorBack = Color(0xFFFF8000)
val colorText = Color(0xFFFFFFFF)

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
        notifications = Notifications(pebble, context).apply {init()}

        BatteryReceiver(pebble).init(context)
        BluetoothReceiver(pebble).init(context)
        WiFiReceiver(pebble).init(context)
        PhoneReceiver(pebble).init(context)

        val intent = Intent(context, PebbleService::class.java)
        context.startForegroundService(intent)

        pebble.askInfo()

        setContent {
            Scaffold(
                topBar = {
                    TopBar(pebble) { stopServices() }
                }
            ) {
                innerPadding ->
                MainPage(
                    pebble = pebble,
                    notifications = notifications,
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

    @Composable
    private fun TopBar(
        pebble: Pebble,
        stopFunction: () -> Unit
    ) {
        val isConnected: Boolean by pebble.isConnected.collectAsState(false)

        TopAppBar(
            title = {
                Text(
                    text = "Pebble",
                    color = colorText,
                    fontSize = titleSize
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = colorBack,
            ),
            actions = {
                if (isConnected) {
                    IconButton(
                        onClick = { stopFunction() }
                    ) {
                        Icon(
                            painterResource(R.drawable.baseline_close_24),
                            contentDescription = null,
                            tint = colorText
                        )
                    }
                }
            }
        )
    }

    @Composable
    private fun MainPage(
        pebble: Pebble,
        notifications: Notifications,
        modifier: Modifier = Modifier,
    ) {

        Column(
            modifier = modifier,
        ) {
            Watchface(pebble)
            AwayTimezone(pebble.timezone)
            Box(Modifier.height(padSize))
            NotificationsList(notifications)
        }
    }

    @Composable
    private fun Section(text: String = "") {
        Text(
            text = text,
            fontSize = titleSize,
            modifier = Modifier.fillMaxWidth()
        )
    }

    @Composable
    private fun Watchface(
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
                    onClick = { pebble.askInfo() },
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
    private fun AwayTimezone(
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
    private fun NotificationsList(
        notifications: Notifications
    ) {
        val map: CharToString by notifications.mapFlow.collectAsState(mutableMapOf<Char, String>())

        Section("Notifications")
        for (item in map) {
            NotificationLine(item.key, item.value)
        }
        if (map.size < 9) NotificationAdd()
    }

    @Composable
    private fun NotificationAdd() {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { resetMap() },
            ) {
                Text(
                    text = "Reset",
                    fontSize = textSize
                )
            }
            Button(
                onClick = { registerMap('?', "") },
            ) {
                Text(
                    text = "Add",
                    fontSize = textSize
                )
            }
        }
    }

    @Composable
    private fun NotificationLine(
        letter: Char,
        packageName: String
    ) {
        var doEdit by remember { mutableStateOf(false) }
        var key by remember { mutableStateOf(letter) }
        var value by remember { mutableStateOf(packageName) }

        Row (
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(4.dp)
        ){
            if (doEdit) {
                OutlinedTextField(
                    value = key.toString(),
                    onValueChange = { key = if (it.isNotEmpty()) it.uppercase().last() else ' ' },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = titleSize,
                        color = colorText,
                    ),
                    modifier = Modifier.fillMaxWidth(.15f).background(colorBack).padding(horizontal = 0.dp)
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = textSize,
                    ),
                    modifier = Modifier.fillMaxWidth(.85f).padding(horizontal = 8.dp)
                )
                FilledIconButton(
                    onClick = {
                        if (key != ' ') registerMap(key, value)
                        doEdit = false
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.outline_check_24,),
                        contentDescription = "Edit",
                    )
                }
            } else {
                Text(
                    text = letter.toString(),
                    fontSize = titleSize,
                    color = colorText,
                    modifier = Modifier.fillMaxWidth(.15f).background(colorBack).padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = packageName,
                    fontSize = textSize,
                    modifier = Modifier.fillMaxWidth(.85f).padding(horizontal = 8.dp)
                )
                FilledIconButton(
                    onClick = { doEdit = true },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.outline_edit_24,),
                        contentDescription = "Edit",
                    )
                }
            }
        }
    }

    private fun registerMap(
        key: Char,
        value: String
    ) {
        val intent = Intent("name.jayhan.pebble.REGISTER_MAP").apply {
            putExtra("key", key.toString())
            putExtra("value", value)
        }
        context.sendBroadcast(intent)
    }

    private fun resetMap() {
        val intent = Intent("name.jayhan.pebble.RESET_MAP")
        context.sendBroadcast(intent)
    }

    @Composable
    fun NotificationLinePreview() {
        Column {
            NotificationLine('S', "com.google.android.messaging")
            NotificationLine('G', "com.google.android.gm")
            NotificationAdd()
        }
    }
}

@Preview
@Composable
fun NotificationLinePreview() {
    MainActivity().NotificationLinePreview()
}
