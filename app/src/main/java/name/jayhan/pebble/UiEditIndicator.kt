package name.jayhan.pebble

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun EditIndicators(
    context: Context,
    letter: Char,
    packageName: String,
    channel: String,
    activeList: List<String>,
    allList: List<String>,
    onClose: () -> Unit
) {
    var newLetter by remember { mutableStateOf(letter) }
    var newPackage by remember { mutableStateOf(packageName) }
    var newChannel by remember { mutableStateOf(channel) }
    var showPackageList by remember { mutableStateOf(false) }

    if (showPackageList) {
        SelectPackage(
            activeList = activeList,
            allList = allList,
            onClose = { showPackageList = false }
        ) { name: String -> newPackage = name }
    } else {
        Dialog(
            onDismissRequest = onClose,
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Top,
                            modifier = Modifier
                                .fillMaxWidth(.5f)
                                .padding(end = 10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.indicator),
                                fontSize = AppConstants.textSize,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp)
                            )
                            OutlinedTextField(
                                value = newLetter.toString(),
                                onValueChange = { newLetter = acceptLetter(it) },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = AppConstants.titleSize,
                                    color = AppConstants.colorText,
                                    textAlign = TextAlign.Center,
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    autoCorrectEnabled = false,
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    cursorColor = AppConstants.colorTransparent
                                ),
                                modifier = Modifier
                                    .background(AppConstants.colorNotiBack)
                                    .padding(horizontal = 8.dp)
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.Top,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = stringResource(R.string.application),
                                fontSize = AppConstants.textSize,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp)
                            )
                            val icon =
                                if (activeList == PreviewActiveList) null
                                else getApplicationIcon(LocalContext.current, newPackage)
                            if (icon != null) {
                                Image(
                                    bitmap = icon,
                                    contentDescription = newPackage,
                                    alignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showPackageList = true }
                                )
                            } else {
                                TextButton(
                                    onClick = { showPackageList = true }
                                ) {
                                    Text(
                                        text = stringResource(R.string.select_package),
                                        fontSize = AppConstants.textSize,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // PackageName
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.package_name),
                        fontSize = AppConstants.textSize,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = AppConstants.colorBlank)
                    ) {
                        var editPackageName by remember { mutableStateOf(false) }
                        BasicTextField(
                            value = newPackage,
                            onValueChange = { newPackage = it },
                            textStyle = TextStyle(
                                fontSize = AppConstants.textSize,
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                autoCorrectEnabled = false,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                                .onFocusChanged { editPackageName = it.hasFocus }
                                .background(color = AppConstants.colorBlank),
                            decorationBox = { inner ->
                                Box {
                                    if (newPackage.isEmpty() && !editPackageName)
                                        Text(
                                            text = stringResource(R.string.package_name_empty),
                                            fontSize = AppConstants.textSize,
                                            color = AppConstants.colorFade,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp)
                                                .background(color = AppConstants.colorBlank),
                                        )
                                    inner()
                                }
                            }
                        )
                    }

                    // Channel
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.channel_filter),
                        fontSize = AppConstants.textSize,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = AppConstants.colorBlank)
                    ) {
                        var editChannel by remember { mutableStateOf(false) }
                        BasicTextField(
                            value = newChannel,
                            onValueChange = { newChannel = it },
                            textStyle = TextStyle(
                                fontSize = AppConstants.textSize,
                                fontStyle = FontStyle.Italic,
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                autoCorrectEnabled = false,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                                .onFocusChanged { editChannel = it.hasFocus }
                                .background(color = AppConstants.colorBlank),
                            decorationBox = { inner ->
                                if (newChannel.isEmpty() && !editChannel)
                                    Text(
                                        text = stringResource(R.string.channel_filter_empty),
                                        fontSize = AppConstants.textSize,
                                        fontStyle = FontStyle.Italic,
                                        color = AppConstants.colorFade,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp)
                                            .background(color = AppConstants.colorBlank),
                                    )
                                inner()
                            }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                if (newLetter != ' ' && newPackage.isNotEmpty()) {
                                    Indicators.remove(packageName, channel)
                                    Indicators.add(
                                        packageName = newPackage,
                                        channel = newChannel,
                                        letter = newLetter,
                                    )
                                    Notifications.reprocess(context)
                                }
                                onClose()
                            },
                        ) {
                            Text(
                                text = stringResource(R.string.save),
                                fontSize = AppConstants.textSize
                            )
                        }
                        Button(
                            onClick = {
                                Indicators.remove(packageName, channel)
                                Notifications.reprocess(context)
                                onClose()
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.remove),
                                fontSize = AppConstants.textSize
                            )
                        }
                    }
                }
            }
        }
    }
}

fun acceptLetter(input: String): Char {
    if (input.isEmpty()) return ' '

    val letter = input.removePrefix(" ").removeSuffix(" ").last()
    if (letter.code >= '!'.code && letter.code <= '~'.code) return letter

    return ' '
}

@Preview
@Composable
fun EditIndicatorsPreview() {
    EditIndicators(
        context = LocalContext.current,
        letter = 'S',
        packageName = "com.android.google.apps.messaging",
        channel = "jayhan.dev",
        activeList = PreviewActiveList,
        allList = listOf()
    ) { }
}

@Preview
@Composable
fun EditIndicatorsEmpty() {
    EditIndicators(
        context = LocalContext.current,
        letter = ' ',
        packageName = "",
        channel = "",
        activeList = PreviewActiveList,
        allList = listOf()
    ) { }
}
