@file:OptIn(ExperimentalMaterial3Api::class)

package name.jayhan.pebble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.telephony.TelephonyManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresPermission
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
    private lateinit var pebble: Pebble
    private lateinit var timezone: Timezone
    private lateinit var notifications: Notifications

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pebble = Pebble(applicationContext)
        timezone = Timezone(pebble)
        notifications = Notifications(pebble)
        notifications.register('S', "com.google.android.apps.messaging")

        val receiverFlagsCompat = ContextCompat.RECEIVER_EXPORTED

        val dataReceiver = DataReceiver(pebble, timezone)
        val dataFilter = IntentFilter(INTENT_APP_RECEIVE)
        ContextCompat.registerReceiver(applicationContext, dataReceiver, dataFilter, receiverFlagsCompat)

        val batteryReceiver = BatteryReceiver(pebble)
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ContextCompat.registerReceiver(applicationContext, batteryReceiver, batteryFilter, receiverFlagsCompat)

        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED) {
            println(0)
        }

        val bluetoothManager: BluetoothManager = applicationContext.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        val bluetoothReceiver = BluetoothReceiver(pebble)
        val bluetoothFilter = IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        ContextCompat.registerReceiver(applicationContext, bluetoothReceiver, bluetoothFilter, receiverFlagsCompat)

        val connMan = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = WiFiCallback(connMan, pebble)
        connMan.registerDefaultNetworkCallback(networkCallback)

        val teleMan = applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        val filter = IntentFilter()
        filter.addAction("name.jayhan.pebble.STATUS_BAR_NOTIFICATIONS")
        registerReceiver(notifications, filter, Context.RECEIVER_EXPORTED)

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
        AwayTimezone(pebble, timezone)
        Box(Modifier.height(padSize))
        NotificationsList()
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
fun AwayTimezone(
    pebble: Pebble,
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
}
