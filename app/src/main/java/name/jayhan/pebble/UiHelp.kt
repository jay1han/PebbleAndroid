package name.jayhan.pebble

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay
import name.jayhan.pebble.ui.theme.PebbleTheme
import kotlin.time.Clock
import kotlin.time.Instant

@Composable
fun HelpDialog(
    watchInfo: WatchInfo,
    lastReceived: Instant,
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
                    .fillMaxWidth()
                    .padding(10.dp)
                    .verticalScroll(scrollState),
            ) {
                Text(
                    text = "${watchInfo.modelString()} (${watchInfo.versionString()})",
                    fontSize = AppConst.titleSize,
                )

                var clockNow by remember { mutableStateOf(Clock.System.now()) }
                Text (
                    text = lastReceived.formatTimeSecond() +
                            " (%s)".format((clockNow - lastReceived).formatDurationSeconds()),
                    fontSize = AppConst.textSize,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
                LaunchedEffect(clockNow) {
                    delay(1000)
                    clockNow = Clock.System.now()
                }

                val batteryText = StringBuilder()
                    .append(stringResource(R.string.format_battery)
                        .format(watchInfo.battery))
                if (watchInfo.plugged) {
                    batteryText.append(stringResource(R.string.plugged))
                    if (watchInfo.charging)
                        batteryText.append(stringResource(R.string.and_charging))
                } else batteryText.append(stringResource(R.string.unplugged))

                Text(
                    text = batteryText.toString(),
                    fontSize = AppConst.textSize,
                    modifier = Modifier.fillMaxWidth()
                )

                val historyText = if (historyData.isValid()) {
                    StringBuilder()
                        .append(stringResource(R.string.this_cycle))
                        .append(historyData.lastUnplug.formatTime())
                        .append(" (%s)"
                            .format((clockNow - historyData.lastUnplug).formatDurationShort()))
                        .append("\n")
                        .append(stringResource(R.string.format_cycle_since)
                            .format(historyData.numberOfCycles))
                        .append(historyData.initDate.formatDate())
                        .append("\n")
                        .append(stringResource(R.string.format_rate)
                            .format(
                                historyData.dischargeRate,
                                100f / historyData.dischargeRate
                            ))
                        .append("\n")
                        .append(stringResource(R.string.format_estimate)
                            .format((watchInfo.battery.toFloat() - 10f) / historyData.dischargeRate))
                        .toString()
                } else {
                    stringResource(R.string.battery_invalid)
                }

                Text(
                    text = historyText,
                    fontSize = AppConst.smallSize,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { confirmClear = true },
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = "Clear history",
                            fontSize = AppConst.textSize,
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.android_github),
                    fontSize = AppConst.smallSize,
                )
                Text(
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxSize(),
                    fontSize = AppConst.smallSize,
                    text = buildAnnotatedString {
                        withLink(
                            LinkAnnotation.Url(AppConst.GITHUB_ANDROID)
                        ) {
                            append(AppConst.GITHUB_ANDROID)
                        }
                    }
                )
                Text(
                    text = stringResource(R.string.pebble_github),
                    fontSize = AppConst.smallSize,
                )
                Text(
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxSize(),
                    fontSize = AppConst.smallSize,
                    text = buildAnnotatedString {
                        withLink(
                            LinkAnnotation.Url(AppConst.GITHUB_PEBBLE)
                        ) {
                            append(AppConst.GITHUB_PEBBLE)
                        }
                    }
                )

                Text(
                    modifier = Modifier.padding(vertical = 10.dp),
                    text = stringResource(R.string.built) + ": " +
                            AppConst.buildDateTime,
                    fontSize = AppConst.smallSize
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
                modifier = Modifier.fillMaxWidth().padding(10.dp),
            ) {
                if (historyData.isValid()) {
                    val duration = Clock.System.now() - historyData.initDate
                    val historyText = stringResource(R.string.format_data_since)
                        .format(
                            historyData.initDate.formatDate(),
                            duration.formatDuration()
                        )
                    Text(
                        text = historyText,
                        fontSize = AppConst.textSize,
                        modifier = Modifier.fillMaxWidth().padding(10.dp)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.no_history),
                        fontSize = AppConst.textSize,
                        modifier = Modifier.fillMaxWidth().padding(10.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.clear_battery_history),
                    fontSize = AppConst.titleSize,
                    lineHeight = AppConst.titleSize * 1.2,
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
                            fontSize = AppConst.textSize
                        )
                    }
                    Button(
                        onClick = { onExit() }
                    ) {
                        Text(
                            text = "No",
                            fontSize = AppConst.textSize
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
    Clock.System.now(),
    10,
    4.5f
)

@Preview
@Composable
fun HelpDialogBattery() {
    PebbleTheme {
        HelpDialog(
            watchInfo = PreviewWatchInfo,
            lastReceived = Clock.System.now(),
            historyData = PreviewHistoryData,
        ) {}
    }
}

@Preview
@Composable
fun ConfirmClearPreview() {
    PebbleTheme {
        ClearBatteryDialog(
            historyData = PreviewHistoryData,
            onConfirm = {}
        ) { }
    }
}
