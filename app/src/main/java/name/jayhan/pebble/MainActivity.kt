@file:OptIn(ExperimentalMaterial3Api::class)

package name.jayhan.pebble

import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE


val titleSize = 28.sp
val textSize = 20.sp
val padSize = 8.dp

class MainActivity : ComponentActivity() {
    lateinit var pebble: Pebble
    lateinit var timezone: Timezone
    lateinit var dataReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pebble = Pebble(applicationContext)
        timezone = Timezone(pebble)
        dataReceiver = DataReceiver(pebble, timezone)

        val filter = IntentFilter(INTENT_APP_RECEIVE)
        val receiverFlags = ContextCompat.RECEIVER_EXPORTED
        ContextCompat.registerReceiver(applicationContext, dataReceiver, filter, receiverFlags)

        pebble.askInfo()

        setContent {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                stringResource(R.string.app_title),
                                fontSize = titleSize
                            )
                        }
                    )
                }
            ) { innerPadding ->
                MainPage(
                    pebble = pebble,
                    timezone = timezone,
                    Modifier.padding(innerPadding),
                )
            }
        }
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
fun MainPage(
    pebble: Pebble,
    timezone: Timezone,
    modifier: Modifier = Modifier,
    ) {
    Column(
        modifier = modifier,
    ) {
        Watchface(pebble)
        PermissionsChecked()
        Box(Modifier.height(padSize))
        AwayTimezone(pebble, timezone)
        Box(Modifier.height(padSize))
        NotificationsList()
        Box(Modifier.height(padSize))
        BluetoothDevices()
    }
}

@Composable
fun Watchface(
    pebble: Pebble
) {
    val watchInfo: String by pebble.infoFlow.collectAsState("")

    Column() {
        Section(
            if (pebble.isConnected) "Connected"
            else "Disconnected"
        )
        Text(
            text = watchInfo,
            fontSize = textSize,
        )
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
fun PermissionsChecked() {
    Section("Permissions")
}

@Composable
fun AwayTimezone(
    pebble: Pebble,
    timezone: Timezone,
) {
    val tzWatch: String by timezone.tzFlow.collectAsState(timezone.get())
    var tz by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }

    Row (
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Away ",
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
            Text(
                " hours",
                fontSize = textSize
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
            Text(
                text = "$tzWatch hours",
                modifier = Modifier.width(140.dp),
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
}

@Composable
fun BluetoothDevices() {
    Section("Bluetooth")
}

@Preview(showBackground = true)
@Composable
fun PebblePreview() {
    val pebble = Pebble()
    val timezone = Timezone(pebble)
    MainPage(pebble, timezone)
}
