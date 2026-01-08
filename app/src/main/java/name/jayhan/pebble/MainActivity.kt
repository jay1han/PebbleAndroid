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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val buildDate = Date(BuildConfig.BUILDTIME)
        buildDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(buildDate)

        context = applicationContext
        getNotificationAccess(context)

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
                    TopBar(Pebble) { stopServices() }
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
                IconButton(
                    onClick = { stopFunction() }
                ) {
                    Icon(
                        painterResource(R.drawable.outline_close_24),
                        contentDescription = null,
                        tint = colorText
                    )
                }
            }
        )
    }

    @Composable
    private fun MainPage(
        modifier: Modifier = Modifier,
    ) {

        Column(
            modifier = modifier,
        ) {
            Watchface()
            AwayTimezone()
            Box(Modifier.height(padSize))
            NotificationsList()
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
    private fun Watchface() {
        val watchInfo: String by Pebble.infoFlow.collectAsState("")
        val isConnected: Boolean by Pebble.isConnected.collectAsState(false)

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
                    onClick = { Pebble.askInfo() },
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
    private fun AwayTimezone() {
        val tzWatch: String by Timezone.tzFlow.collectAsState("+0.0")
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
                        tz = Timezone.fromString(tz)
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
    private fun NotificationsList() {
        val map by Notifications.mapFlow.collectAsState(emptyMap())
        var showEditDialog by remember { mutableStateOf(false) }
        var editLetter by remember { mutableStateOf(' ') }
        var editPackageName by remember { mutableStateOf("") }

        if (showEditDialog) {
            EditNotificationItem(
                editLetter,
                editPackageName,
                onClose = { showEditDialog = false }
            )
        }

        Section("Notifications")
        for (item in map.toSortedMap()) {
            NotificationLine(
                item.key,
                item.value,
                onEdit = {
                    editLetter = item.key
                    editPackageName = item.value
                    showEditDialog = true
                })
        }
        if (map.size < 9) {
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
                    onClick = {
                        editLetter = ' '
                        editPackageName = ""
                        showEditDialog = true
                    },
                ) {
                    Text(
                        text = "Add",
                        fontSize = textSize
                    )
                }
            }
        }
    }

    @Composable
    private fun NotificationLine(
        letter: Char,
        packageName: String,
        onEdit: () -> Unit
    ) {
        Row (
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(4.dp)
        ) {
            Text(
                text = letter.toString(),
                fontSize = titleSize,
                color = colorText,
                modifier = Modifier.fillMaxWidth(.1f).background(colorBack).padding(horizontal = 16.dp),
                textAlign = TextAlign.Center,
            )
            Text(
                text = packageName,
                fontSize = textSize,
                modifier = Modifier.fillMaxWidth(.9f).padding(horizontal = 8.dp)
            )
            FilledIconButton(
                onClick = onEdit
            ) {
                Icon(
                    painter = painterResource(R.drawable.outline_edit_24),
                    contentDescription = "Edit",
                )
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
    private fun ListNotifications(
        onDismiss: () -> Unit,
        onSelect: (String) -> Unit
    ) {
        val notificationList by Notifications.listFlow.collectAsState(listOf())

        Dialog(
            onDismissRequest = onDismiss
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select application",
                        fontSize = titleSize
                    )
                    for (packageName in notificationList) {
                        ListItem(
                            modifier = Modifier.padding(0.dp),
                            headlineContent = {
                                TextButton(
                                    onClick = {
                                        onSelect(packageName)
                                        onDismiss()
                                    },
                                    ) {
                                    Text(
                                        text = packageName,
                                        fontSize = textSize
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun EditNotificationItem(
        letter: Char,
        packageName: String,
        onClose: () -> Unit
    ) {
        var key by remember { mutableStateOf(letter) }
        var value by remember { mutableStateOf(packageName) }
        var showList by remember { mutableStateOf(false) }

        if (showList) {
            ListNotifications(
                onDismiss = { showList = false }
            ) { name: String -> value = name }
        } else {
            Dialog(
                onDismissRequest = onClose,
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedTextField(
                            value = key.toString(),
                            onValueChange = { key = if (it.isNotEmpty()) it.uppercase().last() else ' ' },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = titleSize,
                                color = colorText,
                            ),
                            modifier = Modifier.fillMaxWidth(.2f).background(colorBack).padding(0.dp)
                        )

                        Text(
                            text = value,
                            fontSize = textSize,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = { showList = true }
                        ) {
                            Text(
                                text = "Select application",
                                fontSize = textSize
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                if (key != ' ' && value.isNotEmpty())
                                    registerMap(key, value)
                                onClose()
                            },
                        ) {
                            Text(
                                text = "Save",
                                fontSize = textSize
                            )
                        }
                        Button(
                            onClick = {
                                registerMap(' ', value)
                                onClose()
                            }
                        ) {
                            Text(
                                text = "Remove",
                                fontSize = textSize
                            )
                        }
                    }
                }
            }
        }
    }
}
