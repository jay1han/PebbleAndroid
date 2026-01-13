@file:OptIn(ExperimentalMaterial3Api::class)

package name.jayhan.pebble

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun AppScaffold() {
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
    val isConnected: Boolean by Pebble.isConnected.collectAsState(false)

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
                if (isConnected) {
                    Text(
                        text = "?",
                        fontSize = AppConstants.titleSize,
                        color = AppConstants.colorTop
                    )
                } else {
                    Icon(
                        painterResource(R.drawable.outline_warning_24),
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = "Warning",
                        tint = AppConstants.colorWarning
                    )
                }
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
                        text = stringResource(R.string.connected),
                        fontSize = AppConstants.titleSize
                    )
                    Text(
                        text = stringResource(R.string.model) + ": " +
                                watchInfo.model + "\n" +
                                stringResource(R.string.version) + ": " +
                                watchInfo.version,
                        fontSize = AppConstants.textSize,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.disconnected),
                        fontSize = AppConstants.titleSize
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
    val activeList by Notifications.activeFlow.collectAsState(emptyList())
    val allList by Notifications.allFlow.collectAsState(emptyList())
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .fillMaxWidth(),
    ) {
        AwayTimezone { tz ->
            Pebble.fromString(tz)
        }
//        Spacer(Modifier.height(AppConstants.padSize))
        ShowIndicators(activeList, allList)
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
fun AwayTimezone(
    onApply: (String) -> String
) {
    val isConnected: Boolean by Pebble.isConnected.collectAsState(false)
    val tzWatch: String by Pebble.tzFlow.collectAsState("")
    var tz by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Row (
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp)
            .pointerInput(Unit) {
                detectTapGestures { focusManager.clearFocus() }
            }
    ) {
        Text(
            text = stringResource(R.string.timezone),
            fontSize = AppConstants.titleSize,
        )

        OutlinedTextField(
            readOnly = !editing,
            value = if (editing) tz else tzWatch,
            onValueChange = { tz = it },
            modifier = Modifier
                .weight(1f)
                .padding(AppConstants.padSize)
                .focusProperties { canFocus = editing }
                .focusRequester(focusRequester)
                .onFocusChanged { editing = it.hasFocus },
            textStyle = TextStyle(fontSize = AppConstants.titleSize),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = AppConstants.colorBlack
            )
        )

        if (isConnected) {
            Button(
                onClick = {
                    editing = !editing
                    if (editing) {
                        tz = ""
                        focusRequester.requestFocus()
                    } else {
                        tz = onApply(tz)
                        focusManager.clearFocus()
                    }
                },
                modifier = Modifier.padding(AppConstants.padSize)
            ) {
                Text(
                    text =
                        if (editing) stringResource(R.string.apply)
                        else stringResource(R.string.edit),
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