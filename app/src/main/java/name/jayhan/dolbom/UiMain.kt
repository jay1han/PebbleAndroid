@file:OptIn(ExperimentalMaterial3Api::class)

package name.jayhan.dolbom

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import name.jayhan.dolbom.ui.theme.PebbleTheme
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
fun AppScaffold(
    context: Context
) {
    val watchInfo: WatchInfo by Pebble.infoFlow.collectAsState(WatchInfo())
    val isConnected: Boolean by Pebble.isConnected.collectAsState(false)
    val lastReceived: Instant by Pebble.lastReceived.collectAsState(Clock.System.now())
    val permissionsGranted by Permissions.grantFlow.collectAsState(Permissions.allGranted)
    val activeList by Notifications.activeFlow.collectAsState(emptyList())
    val allList by Notifications.allFlow.collectAsState(emptyList())
    val tzWatch: String by Timezone.tzFlow.collectAsState("")
    val indicators by Indicators.allFlow.collectAsState(listOf())
    val historyData: HistoryData by History.historyFlow.collectAsState(HistoryData())
    var showHelp by remember { mutableStateOf(false) }

    if (!permissionsGranted) {
        PermissionsScaffold()

    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                MainTopBar(context, isConnected, watchInfo) {
                    showHelp = true
                }
            },
        ) { innerPadding ->
            if (showHelp) {
                HelpDialog(
                    watchInfo = watchInfo,
                    lastReceived = lastReceived,
                    historyData = historyData,
                ) {
                    showHelp = false
                }
            }

            MainPage(
                context = context,
                activeList = activeList,
                allList = allList,
                indicators = indicators,
                isConnected = isConnected,
                tzWatch = tzWatch,
                modifier = Modifier.padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            )
        }
    }
}

@Composable
fun MainTopBar(
    context: Context,
    isConnected: Boolean,
    watchInfo: WatchInfo,
    onHelp: () -> Unit
) {
    TopAppBar(
        modifier = Modifier.fillMaxWidth()
            .clickable {
                if (isConnected) onHelp()
                else Pebble.restartService(context)
            },
        navigationIcon = {
            Image(
                painterResource(R.drawable.navicon),
                contentDescription = "Logo",
                modifier = Modifier.padding(end = 10.dp).height(40.dp),
            )
        },
        title = {
            Text(
                text = watchInfo.modelString().ifEmpty { "Disconnected" },
                fontSize = Const.titleSize
            )
        },
        actions = {
            if (isConnected) {
                Text(
                    text = "${watchInfo.battery}%",
                    fontSize = Const.titleSize,
                )
            } else {
                Icon(
                    painterResource(R.drawable.outline_refresh_24),
                    contentDescription = "Refresh",
                )
            }
        }
    )
}

@Composable
fun MainPage(
    context: Context,
    activeList: List<String>,
    allList: List<String>,
    indicators: List<SingleIndicator>,
    isConnected: Boolean,
    tzWatch: String,
    modifier: Modifier = Modifier,
) {
    Column (
        modifier = modifier.fillMaxWidth()
    ){
        AwayTimezone(
            isConnected = isConnected,
            tzWatch = tzWatch
        ) { tz ->
            Timezone.fromString(context, tz)
        }
        IndicatorList(
            context = context,
            activeList = activeList,
            allList = allList,
            indicators = indicators,
        )
    }
}

@Composable
fun AwayTimezone(
    isConnected: Boolean,
    tzWatch: String,
    onApply: (String) -> String
) {
    var tz by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    BackHandler(editing) {
        focusManager.clearFocus()
    }

    Row (
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures { focusManager.clearFocus() }
            }
    ) {
        Text(
            text = stringResource(R.string.timezone),
            fontSize = Const.titleSize,
        )

        Box(
            modifier = Modifier.weight(1f)
        ) {
            BasicTextField(
                readOnly = !editing,
                value = if (editing) tz else tzWatch,
                onValueChange = { tz = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .focusRequester(focusRequester)
                    .focusProperties { canFocus = editing }
                    .onFocusChanged { editing = it.hasFocus || it.isFocused },
                textStyle = TextStyle(
                    fontSize = Const.titleSize,
                    lineHeight = Const.titleSize
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )

            if (!editing) {
                Text(
                    text = "",
                    fontSize = Const.titleSize,
                    lineHeight = Const.titleSize,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                        .alpha(0f)
                        .clickable {
                            editing = true
                            tz = ""
                            focusRequester.requestFocus()
                        }
                )
            }
        }

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
            ) {
                Text(
                    text =
                        if (editing) stringResource(R.string.apply)
                        else stringResource(R.string.edit),
                    fontSize = Const.textSize,
                )
            }
        }
    }
}

@Composable
fun Splash(
    modifier : Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Logo",
            modifier = Modifier.fillMaxSize(),
        )
    }
}

val PreviewWatchInfo = WatchInfo(
    model = 1, version = 0x10000,
    battery = 100, plugged = true, charging = true
)

@Preview
@Composable
fun MainTopBarPreview() {
    PebbleTheme {
        MainTopBar(
            LocalContext.current,
            isConnected = true,
            watchInfo = PreviewWatchInfo
        ) {}
    }
}

@Preview
@Composable
fun MainTopBarDisconnected() {
    PebbleTheme {
        MainTopBar(
            LocalContext.current,
            isConnected = false,
            watchInfo = WatchInfo()
        ) {}
    }
}

@Preview
@Composable
fun MainPagePreview() {
    PebbleTheme {
        MainPage(
            context = LocalContext.current,
            activeList = PreviewActiveList,
            allList = PreviewAllList,
            isConnected = true,
            indicators = PreviewIndicators,
            tzWatch = "+8.0",
        )
    }
}

@Preview
@Composable
fun SplashPreview() {
    PebbleTheme {
        Splash()
    }
}