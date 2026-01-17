package name.jayhan.pebble

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.time.Clock

@Composable
fun HelpDialog(
    watchInfo: WatchInfo,
    isConnected: Boolean,
    lastReceived: String,
    historyData: HistoryData,
    onClose: () -> Unit
) {
    val scrollState = rememberScrollState()
    var confirmClear by remember { mutableStateOf(false) }

    if (confirmClear) {
        ClearBatteryDialog(
            historyData = historyData,
            onConfirm = { History.clear() }
        ) { confirmClear = false }
    }

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
                                watchInfo.modelString() + "\n" +
                                stringResource(R.string.version) + ": " +
                                watchInfo.versionString(),
                        fontSize = AppConstants.textSize,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    Text(
                        text = stringResource(R.string.last_seen) + ": " + lastReceived,
                        fontSize = AppConstants.textSize,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        var text = stringResource(R.string.battery) + ": " +
                                watchInfo.battery.toString() + "%\n"
                        if (watchInfo.plugged) {
                            text += stringResource(R.string.plugged)
                            if (watchInfo.charging)
                                text += stringResource(R.string.and_charging)
                        } else text += stringResource(R.string.unplugged)

                        Text(
                            text = text,
                            fontSize = AppConstants.textSize,
                        )
                    }

                    val historyText = if (historyData.isValid()) {
                        "Since " + historyData.initDate.formatDate() +
                                "\n" + stringResource(R.string.format_discharged_d)
                            .format(historyData.numberOfCycles) +
                                "\n" + stringResource(R.string.format_drop_f)
                            .format(historyData.dischargeRate) +
                                "\n" + stringResource(R.string.format_days_f)
                            .format(watchInfo.battery / historyData.dischargeRate)
                    } else {
                        stringResource(R.string.battery_invalid)
                    }

                    Text(
                        text = historyText,
                        fontSize = AppConstants.textSize,
                        modifier = Modifier.padding(top = 10.dp)
                    )

                    Button(
                        onClick = { confirmClear = true },
                    ) {
                        Text(
                            text = "Clear history",
                            fontSize = AppConstants.textSize,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                            LinkAnnotation.Url(AppConstants.GITHUB_ANDROID)
                        ) {
                            append(AppConstants.GITHUB_ANDROID)
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
                            LinkAnnotation.Url(AppConstants.GITHUB_PEBBLE)
                        ) {
                            append(AppConstants.GITHUB_PEBBLE)
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
fun ClearBatteryDialog(
    historyData: HistoryData,
    onConfirm: () -> Unit,
    onExit: () -> Unit,
) {
    Dialog(
        onDismissRequest = { onExit() }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
            ) {
                if (historyData.isValid()) {
                    val duration = Clock.System.now() - historyData.initDate
                    val historySince = "There is history since " + historyData.initDate.formatDate()
                    val historyDuring = "Time span of " + duration.formatDuration()
                    Text(
                        text = historySince,
                        fontSize = AppConstants.textSize,
                        modifier = Modifier.fillMaxWidth().padding(10.dp)
                    )
                    Text(
                        text = historyDuring,
                        fontSize = AppConstants.textSize,
                        modifier = Modifier.fillMaxWidth().padding(10.dp)
                    )
                } else {
                    Text(
                        text = "No valid history",
                        fontSize = AppConstants.textSize,
                        modifier = Modifier.fillMaxWidth().padding(10.dp)
                    )
                }

                Text(
                    text = "Clear battery history?",
                    fontSize = AppConstants.titleSize,
                    modifier = Modifier.fillMaxWidth().padding(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            onConfirm()
                            onExit()
                        }
                    ) {
                        Text(
                            text = "Yes",
                            fontSize = AppConstants.textSize
                        )
                    }
                    Button(
                        onClick = { onExit() }
                    ) {
                        Text(
                            text = "No",
                            fontSize = AppConstants.textSize
                        )
                    }
                }
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

val PreviewHistoryData = HistoryData(
    Clock.System.now(),
    10,
    4.5f
)

@Preview
@Composable
fun HelpDialogBattery() {
    HelpDialog(
        watchInfo = PreviewWatchInfo,
        isConnected = true,
        lastReceived = "Now",
        historyData = PreviewHistoryData,
    ) {}
}

@Preview
@Composable
fun ConfirmClearPreview() {
    ClearBatteryDialog(
        historyData = PreviewHistoryData,
        onConfirm = {}
    ) { }
}

@Preview
@Composable
fun SplashPreview() {
    Splash()
}
