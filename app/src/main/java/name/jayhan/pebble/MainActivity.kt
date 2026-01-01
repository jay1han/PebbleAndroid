@file:OptIn(ExperimentalMaterial3Api::class)

package name.jayhan.pebble

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.service.notification.Condition
import android.service.notification.ZenPolicy
import android.telephony.TelephonyManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
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
import androidx.core.content.ContextCompat
import com.getpebble.android.kit.Constants.INTENT_APP_RECEIVE


val titleSize = 28.sp
val textSize = 20.sp
val padSize = 8.dp

class MainActivity : ComponentActivity() {
    private lateinit var pebble: Pebble
    private lateinit var timezone: Timezone
    private lateinit var notifications: Notifications

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = getIntent()
        if (intent.action == null || intent.action == NotificationManager.ACTION_AUTOMATIC_ZEN_RULE) {
            finish()
            return
        }

        pebble = Pebble(applicationContext)
        timezone = Timezone(pebble)
        notifications = Notifications(pebble)

        // TODO: Ask runtime permissions

        val notiMan = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val zenUri = Uri.Builder()
            .scheme(Condition.SCHEME)
            .appendPath("jayhan.name")
            .query("dnd")
            .build()
        val zenPolicy = ZenPolicy.Builder()
            .disallowAllSounds()
            .allowAlarms(true)
            .allowCalls(ZenPolicy.PEOPLE_TYPE_STARRED)
            .showAllVisualEffects()
            .build()
        val zenRule = AutomaticZenRule.Builder("pebble", zenUri)
            .setTriggerDescription("Toggled via Pebble watch")
            .setType(AutomaticZenRule.TYPE_OTHER)
            .setManualInvocationAllowed(true)
            .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            .setEnabled(true)
            .setZenPolicy(zenPolicy)
            .setConfigurationActivity(ComponentName(applicationContext, MainActivity::class.java))
            .build()

        var ruleId = ""
        for (rule in notiMan.automaticZenRules) {
            if (rule.value.name == "pebble") {
                ruleId = rule.key
                break
            }
        }

        if (ruleId == "") {
            try {
                ruleId = notiMan.addAutomaticZenRule(zenRule)
            } catch (e: Exception) {
                println(e)
            }
        } else {
            notiMan.updateAutomaticZenRule(ruleId, zenRule)
            notiMan.setAutomaticZenRuleState(ruleId, Condition(zenUri, "Disabled", Condition.STATE_FALSE))
        }

        val receiverFlagsCompat = ContextCompat.RECEIVER_EXPORTED

        val dataReceiver = DataReceiver(pebble, timezone)
        val dataFilter = IntentFilter(INTENT_APP_RECEIVE)
        ContextCompat.registerReceiver(applicationContext, dataReceiver, dataFilter, receiverFlagsCompat)

        // TODO: Read init battery state
        val batteryReceiver = BatteryReceiver(pebble)
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ContextCompat.registerReceiver(applicationContext, batteryReceiver, batteryFilter, receiverFlagsCompat)

        // TODO: Read init bluetooth device
        val bluetoothReceiver = BluetoothReceiver(pebble)
        val bluetoothFilter = IntentFilter(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        ContextCompat.registerReceiver(applicationContext, bluetoothReceiver, bluetoothFilter, receiverFlagsCompat)

        // TODO: Read init WiFi SSID
        val connMan = applicationContext.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = WiFiCallback(connMan, pebble)
        connMan.registerDefaultNetworkCallback(networkCallback)

        // TODO: Read init mobile network
        val teleMan = applicationContext.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        val mobileCallback = MobileCallback(teleMan, pebble)
        teleMan.registerTelephonyCallback(TelephonyManager.INCLUDE_LOCATION_DATA_FINE, applicationContext.mainExecutor, mobileCallback)

        // TODO: Read init notifications list
        val filter = IntentFilter()
        filter.addAction("name.jayhan.pebble.STATUS_BAR_NOTIFICATIONS")
        registerReceiver(notifications, filter, RECEIVER_EXPORTED)

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
        AwayTimezone(timezone)
        Box(Modifier.height(padSize))
        NotificationsList()
    }
}

@Composable
fun Watchface(
    pebble: Pebble
) {
    val watchInfo: String by pebble.infoFlow.collectAsState("")

    Column {
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
