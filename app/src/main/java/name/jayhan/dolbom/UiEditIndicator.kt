package name.jayhan.dolbom

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun EditIndicator(
    context: Context,
    indicator: SingleIndicator,
    activeList: List<String>,
    allList: List<String>,
    onClose: () -> Unit
) {
    var newPackage by remember { mutableStateOf(indicator.packageName) }
    var newChannel by remember { mutableStateOf(indicator.channel) }
    var newText by remember { mutableStateOf(indicator.filterText) }
    var newType by remember { mutableStateOf(indicator.filterType) }
    var newLetter by remember { mutableStateOf(indicator.letter) }
    var showPackageList by remember { mutableStateOf(false) }
    var ignore by remember { mutableStateOf(indicator.ignore) }
    var indicatorError by remember { mutableStateOf(false) }

    if (showPackageList) {
        SelectPackage(
            activeList = activeList,
            allList = allList,
            onClose = { showPackageList = false }
        ) { name: String -> newPackage = name }
        return
    }
    
    if (indicatorError) {
        IndicatorError { indicatorError = false }
    }
    
    Dialog(
        onDismissRequest = onClose,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(0.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth(.4f)
                    ) {
                        val icon: ImageBitmap? =
                            if (activeList == PreviewActiveList) null
                            else getApplicationIcon(LocalContext.current, newPackage)
                        if (icon != null) {
                            Image(
                                bitmap = icon,
                                contentDescription = newPackage,
                                alignment = Alignment.Center,
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { showPackageList = true }
                            )
                        } else {
                            TextButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { showPackageList = true }
                            ) {
                                Text(
                                    text = stringResource(R.string.select_package),
                                    fontSize = Const.textSize,
                                    lineHeight = Const.titleSize,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                    ) {
                        Text(
                            text = "Indicator",
                            fontSize = Const.titleSize,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 10.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                        ) {
                            OutlinedTextField(
                                enabled = !ignore,
                                value = if (ignore) " " else newLetter.toString(),
                                onValueChange = {
                                    newLetter = if (ignore) ' ' else acceptLetter(it)
                                },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = Const.titleSize,
                                    textAlign = TextAlign.Center,
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    autoCorrectEnabled = false,
                                ),
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.ignore_indication),
                                    fontSize = Const.textSize,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                                Switch(
                                    checked = ignore,
                                    onCheckedChange = { state ->
                                        ignore = state
                                        if (ignore) newLetter = ' '
                                    }
                                )
                            }
                        }
                    }
                }

                // PackageName
                var editPackageName by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value =
                        if (newPackage.isEmpty() && !editPackageName)
                            stringResource(R.string.package_empty)
                        else newPackage,
                    onValueChange = { newPackage = it },
                    textStyle = TextStyle(
                        fontSize = Const.textSize,
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        autoCorrectEnabled = false,
                    ),
                    modifier = Modifier.fillMaxWidth()
                        .onFocusChanged { editPackageName = it.isFocused },
                )

                // Channel
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.channel_filter),
                        fontSize = Const.textSize,
                        modifier = Modifier.padding(end = 10.dp)
                    )

                    var editChannel by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value =
                            if (newChannel.isEmpty() && !editChannel)
                                stringResource(R.string.filter_empty)
                            else newChannel,
                        onValueChange = { newChannel = it },
                        textStyle = TextStyle(
                            fontSize = Const.textSize,
                            fontWeight = FontWeight.Bold,
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            autoCorrectEnabled = false,
                        ),
                        modifier = Modifier.fillMaxWidth()
                            .onFocusChanged { editChannel = it.isFocused },
                        maxLines = 1,
                    )
                }

                // Filters
                Spacer(Modifier.height(8.dp))
                var editFilter by remember { mutableStateOf(false) }

                val labels = FilterType.Strings
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    labels.forEachIndexed { index, label ->
                        SegmentedButton(
                            onClick = { newType = FilterType.index(index) },
                            selected = index == newType.ordinal,
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = labels.size
                            )
                        ) {
                            Text(
                                text = stringResource(label),
                                maxLines = 1,
                                autoSize = TextAutoSize.StepBased()
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value =
                        if (newText.isEmpty() && !editFilter)
                            stringResource(R.string.filter_empty)
                        else newText,
                    onValueChange = { newText = it },
                    textStyle = TextStyle(
                        fontSize = Const.textSize,
                        fontStyle = FontStyle.Italic,
                    ),
                    keyboardOptions = KeyboardOptions(
                        autoCorrectEnabled = false,
                    ),
                    modifier = Modifier.fillMaxWidth()
                        .onFocusChanged { editFilter = it.isFocused },
                    maxLines = 1,
                )

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            if (newLetter == ' ' && !ignore) indicatorError = true
                            else {
                                if (newPackage.isNotEmpty()) {
                                    Indicators.remove(indicator)
                                    Indicators.add(
                                        SingleIndicator(
                                            packageName = newPackage,
                                            channel = newChannel,
                                            filterText = newText,
                                            filterType = newType,
                                            letter = if (ignore) ' ' else newLetter,
                                            ignore = ignore,
                                        )
                                    )
                                    Notifications.refresh(context)
                                }
                                onClose()
                            }
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            fontSize = Const.textSize
                        )
                    }
                    Button(
                        onClick = {
                            Indicators.remove(indicator)
                            Notifications.refresh(context)
                            onClose()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.remove),
                            fontSize = Const.textSize
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IndicatorError(
    onExit: () -> Unit
) {
    Dialog(
        onDismissRequest = { onExit() }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(0.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(10.dp),
            ) {
                Text(
                    text = "Indicator must be a visible letter",
                    fontSize = Const.textSize,
                    modifier = Modifier.fillMaxWidth().padding(10.dp)
                )
                Button(
                    onClick = onExit
                ) {
                    Text(
                        text = "OK",
                        fontSize = Const.textSize
                    )
                }
            }
        }
    }
}

fun acceptLetter(input: String): Char {
    if (input.isEmpty()) return ' '

    val letter = try {
        input.removePrefix(" ").removeSuffix(" ").last()
    } catch (_: NoSuchElementException) {
        ' '
    }
    if (letter.code >= '!'.code && letter.code <= '~'.code) return letter

    return ' '
}

@Preview
@Composable
fun EditIndicatorPreview() {
    EditIndicator(
        context = LocalContext.current,
        indicator = SingleIndicator(
            packageName = "com.android.google.apps.messaging",
            channel = "jayhan.dev",
            filterText = "text",
            filterType = FilterType.Subtitle,
            letter = 'S',
            ignore = false
        ),
        activeList = PreviewActiveList,
        allList = listOf()
    ) { }
}

@Preview
@Composable
fun EditIndicatorIgnore() {
    EditIndicator(
        context = LocalContext.current,
        indicator = SingleIndicator(
            packageName = "com.android.google.apps.messaging",
            ignore = false
        ),
        activeList = PreviewActiveList,
        allList = listOf()
    ) { }
}

@Preview
@Composable
fun EditIndicatorEmpty() {
    EditIndicator(
        context = LocalContext.current,
        indicator = SingleIndicator(ignore = false),
        activeList = PreviewActiveList,
        allList = listOf()
    ) { }
}

@Preview
@Composable
fun IndicatorErrorPreview() {
    IndicatorError {}
}
