package name.jayhan.dolbom

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import name.jayhan.dolbom.ui.theme.PebbleTheme
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.isDistantPast

@Composable
fun HelpDialog(
    watchInfo: WatchInfo,
    lastReceived: Instant,
    historyData: HistoryData,
    onClose: () -> Unit
) {
    val scrollState = rememberScrollState()
    var confirmClear by remember { mutableStateOf(false) }
    var showDump by remember { mutableStateOf(false) }
    
    if (showDump) {
        DumpDialog(Notifications.dump) {
            showDump =false
        }
    }

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
                modifier = Modifier.fillMaxWidth().padding(10.dp)
                    .verticalScroll(scrollState),
            ) {
                Text(
                    text = "${watchInfo.modelString()} (${watchInfo.versionString()})",
                    fontSize = Const.titleSize,
                )

                var clockNow by remember { mutableStateOf(Clock.System.now()) }
                Text (
                    text = lastReceived.formatTimeSecond() +
                            " (${(clockNow - lastReceived).formatDurationSeconds()})",
                    fontSize = Const.textSize,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
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
                    fontSize = Const.textSize,
                    modifier = Modifier.fillMaxWidth()
                )

                val historyText = StringBuilder()
                
                if (!historyData.cycleDate.isDistantPast) {
                    historyText.append(stringResource(R.string.format_cycle)
                        .format(historyData.cycleLevel,
                            historyData.cycleDate.formatTime(),
                            (clockNow - historyData.cycleDate).formatDurationMinutes()
                        ))
                    if (historyData.cycleRate > 0f) {
                        historyText.apply {
                            append("\n")
                            append(stringResource(R.string.format_rate)
                                .format(
                                    historyData.cycleRate,
                                    100f / historyData.cycleRate
                                ))
                            append("\n")
                        }
                        val estimate = (watchInfo.battery.toFloat() - 10f) / historyData.cycleRate
                        if (estimate > 0) {
                            historyText.append(stringResource(R.string.format_estimate).format(estimate))
                        } else {
                            historyText.append("Please recharge")
                        }
                    }
                } else historyText.append("No past data")

                Text(
                    text = historyText.toString(),
                    fontSize = Const.smallSize,
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
                            fontSize = Const.textSize,
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.android_github),
                    fontSize = Const.smallSize,
                )
                Text(
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxSize(),
                    fontSize = Const.smallSize,
                    text = buildAnnotatedString {
                        withLink(
                            LinkAnnotation.Url(Const.GITHUB_ANDROID)
                        ) {
                            append(Const.GITHUB_ANDROID)
                        }
                    }
                )
                Text(
                    text = stringResource(R.string.pebble_github),
                    fontSize = Const.smallSize,
                )
                Text(
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxSize(),
                    fontSize = Const.smallSize,
                    text = buildAnnotatedString {
                        withLink(
                            LinkAnnotation.Url(Const.GITHUB_PEBBLE)
                        ) {
                            append(Const.GITHUB_PEBBLE)
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.padding(8.dp).weight(1f),
                        text = stringResource(R.string.built) + Const.buildDateTime,
                        fontSize = Const.smallSize
                    )
                    Button(
                        onClick = { showDump = true }
                    ) {
                        Text(
                            text = "Dump",
                            fontSize = Const.smallSize
                        )
                    }
                }
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
                if (!historyData.historyDate.isDistantPast) {
                    val duration = Clock.System.now() - historyData.historyDate
                    val historyText = stringResource(R.string.format_data_since)
                        .format(
                            historyData.historyCycles,
                            historyData.historyDate.formatDate(),
                            duration.formatDuration()
                        )
                    Text(
                        text = historyText,
                        fontSize = Const.textSize,
                        modifier = Modifier.fillMaxWidth().padding(10.dp)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.no_history),
                        fontSize = Const.textSize,
                        modifier = Modifier.fillMaxWidth().padding(10.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.clear_battery_history),
                    fontSize = Const.titleSize,
                    lineHeight = Const.titleSize * 1.2,
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
                            fontSize = Const.textSize
                        )
                    }
                    Button(
                        onClick = { onExit() }
                    ) {
                        Text(
                            text = "No",
                            fontSize = Const.textSize
                        )
                    }
                }
            }
        }
    }
}

val PreviewHistoryData = HistoryData(
    historyDate = Clock.System.now(),
    historyCycles = 10,
    historyRate = 4.5f,
    cycleDate = Clock.System.now(),
    cycleLevel = 80,
    cycleRate =7.5f,
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
