@file:OptIn(ExperimentalMaterial3Api::class)

package name.jayhan.pebble

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun TopBar(
    stopFunction: () -> Unit
) {
    val isConnected: Boolean by Pebble.isConnected.collectAsState(false)

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
fun MainPage(
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
fun Section(text: String = "") {
    Text(
        text = text,
        fontSize = titleSize,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun Watchface() {
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
fun AwayTimezone() {
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
