@file:OptIn(ExperimentalMaterial3Api::class)

package name.jayhan.pebble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.os.BatteryManager
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
import com.getpebble.android.kit.util.PebbleDictionary
import kotlin.reflect.typeOf


val titleSize = 28.sp
val textSize = 20.sp
val padSize = 8.dp

class BatteryReceiver(pebble: Pebble): BroadcastReceiver() {
    val pebble = pebble

    override fun onReceive(context: Context, intent: Intent) {
        val isCharging = intent.getIntExtra(BatteryManager.EXTRA_CHARGING_STATUS, 0)
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0)
        val percent = 100.0 * level.toFloat() / scale

        var pebbleDict = PebbleDictionary()
        pebbleDict.addInt8(DictKey.MSG_TYPE.code, MsgType.PHONE_CHG.code)
        pebbleDict.addInt8(DictKey.PHONE_CHG.code, isCharging.toByte())
        pebbleDict.addInt8(DictKey.PHONE_BATT.code, percent.toInt().toByte())
        pebble.send(pebbleDict)
    }
}

class BluetoothReceiver(pebble: Pebble): BroadcastReceiver() {
    val pebble = pebble

    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.STATE_DISCONNECTED)
        if (state != BluetoothAdapter.STATE_CONNECTED) {
            val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            if (device != null) {
                print(device.name)
            }
        }
    }
}

class NetworkCallback: ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
        super.onAvailable(network)
    }
}

class MainActivity : ComponentActivity() {
    lateinit var pebble: Pebble
    lateinit var timezone: Timezone

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pebble = Pebble(applicationContext)
        timezone = Timezone(pebble)

        val receiverFlags = ContextCompat.RECEIVER_EXPORTED

        val dataReceiver = DataReceiver(pebble, timezone)
        val dataFilter = IntentFilter(INTENT_APP_RECEIVE)
        ContextCompat.registerReceiver(applicationContext, dataReceiver, dataFilter, receiverFlags)

        val batteryReceiver = BatteryReceiver(pebble)
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ContextCompat.registerReceiver(applicationContext, batteryReceiver, batteryFilter, receiverFlags)

        val bluetoothManager: BluetoothManager = applicationContext.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        val bluetoothState = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.A2DP)
        print(bluetoothState)

        val bluetoothReceiver = BluetoothReceiver(pebble)
        val bluetoothFilter = IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        ContextCompat.registerReceiver(applicationContext, bluetoothReceiver, bluetoothFilter, receiverFlags)

        val networkCallback = NetworkCallback()
        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//        connectivityManager.registerDefaultNetworkCallback(networkCallback)

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

@Preview(showBackground = true)
@Composable
fun PebblePreview() {
    val pebble = Pebble()
    val timezone = Timezone(pebble)
    MainPage(pebble, timezone)
}
