package name.jayhan.pebble

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.FilledIconButton
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.createBitmap

@Composable
fun IndicatorList(
    context: Context,
    activeList: List<String>,
    allList: List<String>,
    indicators: List<SingleIndicator>
) {
    var editDialog by remember { mutableStateOf(false) }
    var editIndicator by remember { mutableStateOf(SingleIndicator()) }
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
            editIndicator = SingleIndicator()
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
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Button(
                onClick = { resetDialog = true },
            ) {
                Text(
                    text = stringResource(R.string.reset),
                    fontSize = AppConstants.textSize
                )
            }
            Text(
                text = stringResource(R.string.indicators),
                fontSize = AppConstants.titleSize,
            )
            Button(
                onClick = {
                    editIndicator = SingleIndicator()
                    editDialog = true
                },
            ) {
                Text(
                    text = stringResource(R.string.add),
                    fontSize = AppConstants.textSize
                )
            }
        }

        if (indicators.isEmpty()) {
            Text(
                text = stringResource(R.string.no_indicators),
                textAlign = TextAlign.Center,
                fontSize = AppConstants.titleSize,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxWidth()
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
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(.1f)
                .padding(4.dp)
        ) {
            val icon = getApplicationIcon(LocalContext.current, indicator.packageName)
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = indicator.packageName,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .weight(1f)
        ) {
            val appName = Notifications.getApplicationName(indicator.packageName)
            if (appName != "") {
                Text(
                    text = appName,
                    fontSize = AppConstants.textSize,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                text = indicator.packageName,
                fontSize = AppConstants.subSize,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (indicator.channel.isNotEmpty()) {
                    Text(
                        text = indicator.channel,
                        fontSize = AppConstants.subSize,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (indicator.filterText.isNotEmpty()) {
                    Text(
                        text = indicator.filterText,
                        fontSize = AppConstants.subSize,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Text(
            text = indicator.letter.toString(),
            fontSize = AppConstants.titleSize,
            color = AppConstants.colorText,
            modifier = Modifier
                .fillMaxWidth(.1f)
                .background(AppConstants.colorNotiBack)
                .padding(horizontal = 16.dp),
            textAlign = TextAlign.Center,
        )

        FilledIconButton(
            onClick = onEdit,
        ) {
            Icon(
                painter = painterResource(R.drawable.outline_edit_24),
                contentDescription = stringResource(R.string.edit),
            )
        }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.reset_question),
                    fontSize = AppConstants.titleSize,
                    modifier = Modifier.padding(10.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = onClose
                    ) {
                        Text(
                            text = stringResource(R.string.reset_no),
                            fontSize = AppConstants.textSize,
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
                            fontSize = AppConstants.textSize,
                        )
                    }
                }
            }
        }
    }
}

fun getApplicationIcon(
    context: Context,
    packageName: String
): ImageBitmap? {
    if (packageName.isEmpty()) return null

    val drawable = try {
        context.packageManager.getApplicationIcon(packageName)
    } catch (_: PackageManager.NameNotFoundException) {
        return null
    }

    val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap.asImageBitmap()
}

val PreviewActiveList = listOf(
    "com.android.google.apps.messaging",
    "com.android.google.apps.messaging",
    "com.whatsapp"
)

val PreviewAllList = listOf(
    "com.android.google.apps.messaging",
    "com.android.google.apps.gm",
    "com.whatsapp"
)

@Preview
@Composable
fun IndicatorItemPreview() {
    IndicatorItem(
        SingleIndicator(
            "com.google.android.apps.messaging",
            "jayhan.dev",
            "",
            'S'),
    ) { }
}

@Preview
@Composable
fun IndicatorListPreview() {
    IndicatorList(
        context = LocalContext.current,
        activeList = PreviewActiveList,
        allList = PreviewAllList,
        indicators = PreviewIndicators
    )
}

@Preview
@Composable
fun IndicatorListEmpty() {
    IndicatorList(
        context = LocalContext.current,
        activeList = PreviewActiveList,
        allList = PreviewAllList,
        indicators = listOf(),
    )
}

@Preview
@Composable
fun ResetDialogPreview() {
    ResetDialog(
        onClose = {},
        onConfirm = {}
    )
}