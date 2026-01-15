@file:OptIn(ExperimentalMaterial3Api::class)

package name.jayhan.pebble

import android.content.Context
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
fun AppScaffold(
    context: Context
) {
    val watchInfo: WatchInfo by Pebble.infoFlow.collectAsState(WatchInfo())
    val isConnected: Boolean by Pebble.isConnected.collectAsState(false)
    val lastReceived: Instant by Pebble.lastReceived.collectAsState(Instant.DISTANT_PAST)
    val permissionsGranted by Permissions.grantFlow.collectAsState(Permissions.allGranted)
    val serverUp by Permissions.initFlow.collectAsState(false)
    val activeList by Notifications.activeFlow.collectAsState(emptyList())
    val allList by Notifications.allFlow.collectAsState(emptyList())
    val tzWatch: String by Pebble.tzFlow.collectAsState("")
    val indicators by Indicators.allFlow.collectAsState(mapOf())
    var showHelp by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopBar(isConnected) {
                showHelp = true
            }
        },
    ) { innerPadding ->
        if (serverUp) {
            if (permissionsGranted) {
                if (showHelp) {
                    HelpDialog(
                        watchInfo = watchInfo,
                        isConnected = isConnected,
                        lastReceived = lastReceived,
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
                )

            } else {
                val missingList by Permissions.missingFlow.collectAsState(listOf())

                UiPermissions(
                    missingList,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        } else {
            Splash(
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
fun TopBar(
    isConnected: Boolean,
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
fun HelpDialog(
    watchInfo: WatchInfo,
    isConnected: Boolean,
    lastReceived: Instant,
    onClose: () -> Unit
) {
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onClose
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxWidth()
                    .padding(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.connected),
                    fontSize = AppConstants.titleSize,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                if (isConnected) {
                    Text(
                        text = stringResource(R.string.model) + ": " +
                                watchInfo.model + "\n" +
                                stringResource(R.string.version) + ": " +
                                watchInfo.version,
                        fontSize = AppConstants.textSize,
                    )
                    Text(
                        text = stringResource(R.string.last_seen) + ": " +
                                lastReceived.formatDate(),
                        fontSize = AppConstants.textSize,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.android_github),
                    fontSize = AppConstants.smallSize,
                )
                Text(
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxSize(),
                    fontSize = AppConstants.smallSize,
                    text = buildAnnotatedString {
                        withLink(
                            LinkAnnotation.Url(AppConstants.GithubAndroid)
                        ) {
                            append(AppConstants.GithubAndroid)
                        }
                    }
                )
                Text(
                    text = stringResource(R.string.pebble_github),
                    fontSize = AppConstants.smallSize,
                )
                Text(
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxSize(),
                    fontSize = AppConstants.smallSize,
                    text = buildAnnotatedString {
                        withLink(
                            LinkAnnotation.Url(AppConstants.GithubPebble)
                        ) {
                            append(AppConstants.GithubPebble)
                        }
                    }
                )

                Text(
                    modifier = Modifier.padding(vertical = 10.dp),
                    text = stringResource(R.string.built) + ": " +
                            AppConstants.buildDateTime,
                    fontSize = AppConstants.smallSize
                )
            }
        }
    }
}

@Composable
fun MainPage(
    context: Context,
    activeList: List<String>,
    allList: List<String>,
    indicators: Map<String, Char>,
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
            Pebble.fromString(context, tz)
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

@Composable
fun Splash(
    modifier : Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize()
    ) {
        // TODO: Nice image
        Text(
            text = "Splash",
        )
    }
}

@Preview
@Composable
fun TopBarPreview() {
    TopBar(
        isConnected = false
    ) {}
}

@Preview
@Composable
fun MainPagePreview() {
    MainPage(
        context = LocalContext.current,
        activeList = PreviewActiveList,
        allList = PreviewAllList,
        isConnected = true,
        indicators = PreviewIndicators,
        tzWatch = "+8.0",
    )
}

@Preview
@Composable
fun HelpDialogPreview() {
    HelpDialog(
        watchInfo = WatchInfo("model", "version"),
        isConnected = true,
        lastReceived = Clock.System.now()
    ) {}
}

@Preview
@Composable
fun SplashPreview() {
    Splash()
}