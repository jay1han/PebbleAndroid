@file:OptIn(ExperimentalMaterial3Api::class)

package name.jayhan.pebble

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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun AppScaffold(
    onQuit: () -> Unit
) {
    val permissionsGranted by Permissions.grantFlow.collectAsState(Permissions.allGranted)
    var showHelp by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopBar {
                showHelp = true
            }
        }
    ) { innerPadding ->
        if (permissionsGranted) {
            if (showHelp) {
                ShowHelp(
                    modifier = Modifier.padding(innerPadding)
                ) {
                    showHelp = false
                }
            }
            MainPage(
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
    onHelp: () -> Unit
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
                onClick = { onHelp() }
            ) {
                Text(
                    text = "?",
                    fontSize = AppConstants.titleSize,
                    color = AppConstants.colorBlank
                )
            }
        }
    )
}

@Composable
fun ShowHelp(
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    val scrollState = rememberScrollState()
    val watchInfo: WatchInfo by Pebble.infoFlow.collectAsState(WatchInfo())
    val isConnected: Boolean by Pebble.isConnected.collectAsState(false)

    Dialog(
        onDismissRequest = onClose
    ) {
        Card(
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxWidth()
                    .padding(10.dp),
            ) {
                if (isConnected) {
                    Text(
                        text = stringResource(R.string.model) + ": " +
                                watchInfo.model + "\n" +
                                stringResource(R.string.version) + ": " +
                                watchInfo.version,
                        fontSize = AppConstants.textSize,
                    )
                }
                Text(
                    text = stringResource(R.string.built) + AppConstants.buildDateTime,
                    fontSize = AppConstants.smallSize
                )
            }
        }
    }
}

@Composable
fun MainPage(
    modifier: Modifier = Modifier,
) {
    val activeList by Notifications.activeFlow.collectAsState(Notifications.activeList)
    val allList by Notifications.allFlow.collectAsState(Notifications.allList)
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.verticalScroll(scrollState).fillMaxWidth(),
    ) {
        Watchface()
        AwayTimezone { tz ->
            Pebble.fromString(tz)
        }
        Box(Modifier.height(AppConstants.padSize))
        MainPackageList(activeList, allList)
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
    modifier: Modifier = Modifier
) {
    val isConnected: Boolean by Pebble.isConnected.collectAsState(false)

    Row(
        modifier = modifier.fillMaxWidth()
    ) {
        Section(
            if (isConnected) stringResource(R.string.connected)
            else stringResource(R.string.disconnected)
        )
    }
}

@Composable
fun AwayTimezone(
    modifier: Modifier = Modifier,
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
fun TopBarPreview() {
    TopBar {}
}

@Preview
@Composable
fun MainPagePreview() {
    MainPage()
}

@Preview
@Composable
fun HelpPreview() {
    ShowHelp {}
}