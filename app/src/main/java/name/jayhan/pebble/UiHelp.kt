package name.jayhan.pebble

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun HelpDialog(
    watchInfo: WatchInfo,
    isConnected: Boolean,
    lastReceived: String,
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
                        Button(
                            onClick = { Pebble.askBattery() },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.outline_refresh_24),
                                contentDescription = "Refresh"
                            )
                        }
                    }
                    
                    // TODO: calculate this asynch
                    Text(
                        text = "Discharged %d times\n".format(0) +
                                "Average rate: %.1f%%/h\n".format(0f) +
                                "Current charge: %.1f hours".format(1f)
                        ,
                        fontSize = AppConstants.textSize,
                        modifier = Modifier.padding(top = 10.dp)
                    )
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
fun HelpDialogPreview() {
    HelpDialog(
        watchInfo = PreviewWatchInfo,
        isConnected = true,
        lastReceived = "Now"
    ) {}
}

@Preview
@Composable
fun SplashPreview() {
    Splash()
}
