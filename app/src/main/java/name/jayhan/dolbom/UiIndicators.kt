package name.jayhan.dolbom

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import name.jayhan.dolbom.ui.theme.PebbleTheme

@Composable
fun IndicatorList(
    context: Context,
    activeList: List<String>,
    allList: List<String>,
    indicators: List<SingleIndicator>
) {
    var editDialog by remember { mutableStateOf(false) }
    var editIndicator by remember { mutableStateOf(SingleIndicator(ignore = false)) }
    val scrollState = rememberScrollState()
    var resetDialog by remember { mutableStateOf(false) }

    if (editDialog) {
        EditIndicator(
            context = context,
            indicator = editIndicator,
            activeList = activeList,
            allList = allList
        ) {
            editDialog = false
            editIndicator = SingleIndicator(ignore = false)
        }
    }

    if (resetDialog) {
        ResetDialog(
            onClose = {
                resetDialog = false
            },
            onConfirm = {
                Indicators.reset()
                Notifications.refresh(context)
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Button(
                onClick = { resetDialog = true },
            ) {
                Text(
                    text = stringResource(R.string.reset),
                    fontSize = Const.textSize
                )
            }
            Text(
                text = stringResource(R.string.indicators),
                fontSize = Const.titleSize,
            )
            Button(
                onClick = {
                    editIndicator = SingleIndicator(ignore = false)
                    editDialog = true
                },
            ) {
                Text(
                    text = stringResource(R.string.add),
                    fontSize = Const.textSize
                )
            }
        }

        if (indicators.isEmpty()) {
            Text(
                text = stringResource(R.string.no_indicators),
                textAlign = TextAlign.Center,
                fontSize = Const.titleSize,
                modifier = Modifier.fillMaxWidth().padding(10.dp)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().padding(0.dp)
                    .verticalScroll(scrollState),
            ) {
                HorizontalDivider(thickness = 1.dp)

                for (indicator in indicators) {
                    IndicatorItem(
                        indicator,
                        onEdit = {
                            editIndicator = indicator
                            editDialog = true
                        }
                    )
                    HorizontalDivider(thickness = 1.dp)
                }
            }
        }
    }
}

@Composable
fun IndicatorItem(
    indicator: SingleIndicator,
    onEdit: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .clickable { onEdit() }
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(.1f).padding(4.dp)
        ) {
            val icon = getApplicationIcon(LocalContext.current, indicator.packageName)
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = indicator.packageName,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = "?",
                    fontSize = Const.titleSize,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 8.dp).weight(1f)
        ) {
            val appName = Notifications.getApplicationName(indicator.packageName)
            if (appName != "") {
                Text(
                    text = appName,
                    fontSize = Const.textSize,
                    lineHeight = Const.textSize,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                text = indicator.packageName,
                fontSize = Const.subSize,
                lineHeight = Const.subSize,
                modifier = Modifier.fillMaxWidth()
            )

            if (indicator.channel.isNotEmpty()) {
                Text(
                    text = indicator.channel,
                    fontSize = Const.subSize,
                    lineHeight = Const.subSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (indicator.filterText.isNotEmpty()) {
                val filterName = stringResource(indicator.filterType.r)
                Text(
                    text = "[$filterName] ${indicator.filterText}",
                    fontSize = Const.subSize,
                    lineHeight = Const.subSize,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.background(color =
                if (indicator.ignore) Color.Transparent else Const.colorIndicatorBack)
        ) {
            Text(
                text = indicator.letter.toString(),
                fontSize = Const.titleSize,
                modifier = Modifier.fillMaxWidth(.1f).padding(horizontal = 16.dp),
                textAlign = TextAlign.Center,
                color = if (indicator.ignore) Color.Transparent else Const.colorIndicatorLetter,
            )
            if (indicator.ignore)
                Image(
                    painter = painterResource(R.drawable.outline_toggle_on_24),
                    contentDescription = "Ignored",
                )
        }

        Icon(
            painterResource(R.drawable.outline_chevron_forward_24),
            contentDescription = "Refresh",
        )
    }
}

@Composable
fun ResetDialog(
    onConfirm: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose
    ) {
        Card {
            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.reset_question),
                    fontSize = Const.titleSize,
                    lineHeight = Const.titleSize * 1.2f,
                    modifier = Modifier.padding(10.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = onClose
                    ) {
                        Text(
                            text = stringResource(R.string.reset_no),
                            fontSize = Const.textSize,
                        )
                    }
                    Button(
                        onClick = {
                            onConfirm()
                            onClose()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.reset_yes),
                            fontSize = Const.textSize,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun IndicatorItemPreview() {
    PebbleTheme {
        IndicatorItem(
            SingleIndicator(
                "com.google.android.apps.messaging",
                "jayhan.dev",
                "text",
                letter = 'S',
                ignore = false
            ),
        ) { }
    }
}

@Preview
@Composable
fun IndicatorListPreview() {
    PebbleTheme {
        IndicatorList(
            context = LocalContext.current,
            activeList = PreviewActiveList,
            allList = PreviewAllList,
            indicators = PreviewIndicators
        )
    }
}

@Preview
@Composable
fun IndicatorListEmpty() {
    PebbleTheme {
        IndicatorList(
            context = LocalContext.current,
            activeList = PreviewActiveList,
            allList = PreviewAllList,
            indicators = listOf(),
        )
    }
}

@Preview
@Composable
fun ResetDialogPreview() {
    PebbleTheme {
        ResetDialog(
            onClose = {},
            onConfirm = {}
        )
    }
}