@file:OptIn(ExperimentalMaterial3Api::class)

package name.jayhan.pebble

import android.content.Context
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.preferencesDataStore
import com.getpebble.android.kit.PebbleKit


class MainActivity : ComponentActivity() {
    val pebble = Pebble(applicationContext)
    val timezone = Timezone(pebble)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                stringResource(R.string.app_title),
                                fontSize = 32.sp
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
        fontSize = 32.sp,
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
        Box(Modifier.height(8.dp))
        AwayTimezone(pebble, timezone)
        Box(Modifier.height(8.dp))
        NotificationsList()
        Box(Modifier.height(8.dp))
        BluetoothDevices()
    }
}

@Composable
fun Watchface(
    pebble: Pebble
) {
    Column() {
        Section(
            if (pebble.isConnected()) "Connected"
            else "Disconnected"
        )
        Image(
            painter = painterResource(R.drawable.help),
            modifier = Modifier.height(200.dp).padding(8.dp).fillMaxWidth(),
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
    var tz by remember { mutableStateOf(timezone.get()) }

    Row (
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Away ",
            fontSize = 32.sp
        )
        OutlinedTextField(
            value = tz,
            onValueChange = {tz = it},
            modifier = Modifier.width(120.dp),
            textStyle = TextStyle(fontSize = 32.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
        )
        Button(
            onClick = {tz = timezone.set(tz)},
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "Apply",
                fontSize = 32.sp,
            )
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
