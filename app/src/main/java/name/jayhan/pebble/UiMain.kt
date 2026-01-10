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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AppScaffold(
    mapper: Mapper,
    onQuit: () -> Unit
) {
    val permissionsGranted by Permissions.grantFlow.collectAsState(Permissions.allGranted)

    Scaffold(
        topBar = {
            TopBar {
                onQuit()
            }
        }
    ) { innerPadding ->
        if (permissionsGranted) {
            MainPage(
                mapper = mapper,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            val missingList by Permissions.missingFlow.collectAsState(listOf())

            UiPermissions(
                missingList,
                Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun TopBar(
    onQuit: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.app_name),
                color = AppConstants.colorText,
                fontSize = AppConstants.titleSize
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppConstants.colorBack,
        ),
        actions = {
            IconButton(
                onClick = { onQuit() }
            ) {
                Icon(
                    painterResource(R.drawable.outline_close_24),
                    contentDescription = null,
                    tint = AppConstants.colorText
                )
            }
        }
    )
}

@Composable
fun MainPage(
    mapper: Mapper,
    modifier: Modifier = Modifier,
) {
    val packageList by Notifications.listFlow.collectAsState(Notifications.activeList)
    val packageMap by Notifications.mapFlow.collectAsState(emptyMap())
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.verticalScroll(scrollState),
    ) {
        Watchface { Pebble.askInfo() }
        AwayTimezone { tz ->
            Pebble.fromString(tz)
        }
        Box(Modifier.height(AppConstants.padSize))
        PackageList(packageMap, mapper, packageList)
    }
}

@Composable
fun Section(text: String = "") {
    Text(
        text = text,
        fontSize = AppConstants.titleSize,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun Watchface(
    onReconnect: () -> Unit
) {
    val watchInfo: WatchInfo by Pebble.infoFlow.collectAsState(WatchInfo())
    val isConnected: Boolean by Pebble.isConnected.collectAsState(false)

    Column {
        Section(
            if (isConnected) stringResource(R.string.connected)
            else stringResource(R.string.disconnected)
        )
        Text(
            text = stringResource(R.string.built) + AppConstants.buildDateTime,
            fontSize = AppConstants.smallSize
        )
        if (isConnected) {
            Text(
                text = stringResource(R.string.model) + ": " +
                        watchInfo.model + "\n" +
                        stringResource(R.string.version) + ": " +
                        watchInfo.version,
                fontSize = AppConstants.textSize,
            )
        } else {
            Button(
                onClick = { onReconnect() },
            ) {
                Text(
                    text = stringResource(R.string.reconnect),
                    fontSize = AppConstants.textSize
                )
            }
        }
        Image(
            painter = painterResource(R.drawable.help),
            modifier = Modifier
                .height(200.dp)
                .padding(AppConstants.padSize)
                .fillMaxWidth(),
            contentScale = ContentScale.Fit,
            contentDescription = "Help",
        )
    }
}

@Composable
fun AwayTimezone(
    onApply: (String) -> String
) {
    val tzWatch: String by Pebble.tzFlow.collectAsState("+0.0")
    var tz by remember { mutableStateOf("+0.0") }
    var editing by remember { mutableStateOf(false) }

    Row (
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Timezone",
            fontSize = AppConstants.titleSize
        )
        if (editing) {
            OutlinedTextField(
                value = tz,
                onValueChange = { tz = it },
                modifier = Modifier.width(100.dp),
                textStyle = TextStyle(fontSize = AppConstants.textSize),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            Button(
                onClick = {
                    editing = false
                    tz = onApply(tz)
                },
                modifier = Modifier.padding(AppConstants.padSize)
            ) {
                Text(
                    text = "Apply",
                    fontSize = AppConstants.textSize,
                )
            }
        } else {
            tz = tzWatch
            Text(
                text = tzWatch,
                modifier = Modifier.width(100.dp),
                fontSize = AppConstants.textSize,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = {editing = true},
                modifier = Modifier.padding(AppConstants.padSize)
            ) {
                Text(
                    text = "Edit",
                    fontSize = AppConstants.textSize,
                )
            }
        }
    }
}

@Preview
@Composable
fun MainPagePreview() {
    val mapper = Mapper(LocalContext.current)
    MainPage(
        mapper = mapper,
        modifier = Modifier
    )
}