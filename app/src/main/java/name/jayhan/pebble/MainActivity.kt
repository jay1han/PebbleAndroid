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

val timezone = Timezone()

class MainActivity : ComponentActivity() {
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
                Pebble(Modifier.padding(innerPadding))
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
fun Pebble(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
    ) {
        Watchface()
        Box(Modifier.height(8.dp))
        PermissionsChecked()
        Box(Modifier.height(8.dp))
        AwayTimezone()
        Box(Modifier.height(8.dp))
        NotificationsList()
        Box(Modifier.height(8.dp))
        BluetoothDevices()
    }
}

@Composable
fun Watchface(modifier: Modifier = Modifier) {
    Column(modifier) {
        Section("Connected")
        Image(
            painter = painterResource(R.drawable.help),
            modifier = modifier.height(200.dp).padding(8.dp).fillMaxWidth(),
            contentScale = ContentScale.Fit,
            contentDescription = "Help",
        )
    }
}

@Composable
fun PermissionsChecked(modifier: Modifier = Modifier) {
    Section("Permissions")
}

@Composable
fun AwayTimezone(modifier: Modifier = Modifier) {
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
            modifier = modifier.width(120.dp),
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
fun NotificationsList(modifier: Modifier = Modifier) {
    Section("Notifications")
}

@Composable
fun BluetoothDevices(modifier: Modifier = Modifier) {
    Section("Bluetooth")
}

@Preview(showBackground = true)
@Composable
fun PebblePreview() {
    Pebble()
}