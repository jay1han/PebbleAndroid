package name.jayhan.pebble

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun EditIndicator(
    indicators: Map<String, Char>,
    letter: Char,
    packageName: String,
    activeList: List<String>,
    allList: List<String>,
    onClose: () -> Unit
) {
    var newLetter by remember { mutableStateOf(letter) }
    var newPackage by remember { mutableStateOf(packageName) }
    var showList by remember { mutableStateOf(false) }
    var editingPackage by remember { mutableStateOf(false) }

    if (showList) {
        SelectPackage(
            activeList = activeList.distinct().filter { !indicators.containsKey(it) },
            allList = allList.filter { !indicators.containsKey(it) },
            onClose = { showList = false }
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
                                if (activeList == PreviewPackageList) null
                                else getApplicationIcon(LocalContext.current, newPackage)
                            if (icon != null) {
                                Image(
                                    bitmap = icon,
                                    contentDescription = newPackage,
                                    alignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showList = true }
                                )
                            } else {
                                TextButton(
                                    onClick = { showList = true }
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

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Package name",
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
                        BasicTextField(
                            value = newPackage,
                            onValueChange = { newPackage = it },
                            textStyle = TextStyle(
                                fontSize = AppConstants.textSize,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                                .onFocusChanged { editingPackage = it.hasFocus }
                                .background(color = AppConstants.colorBlank),
                            decorationBox = { inner ->
                                Box {
                                    if (newPackage.isEmpty() && !editingPackage)
                                        Text(
                                            text = stringResource(R.string.no_package),
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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                if (newLetter != ' ' && newPackage.isNotEmpty())
                                    Notifications.register(newLetter, newPackage)
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
                                Notifications.remove(newPackage)
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
fun EditIndicatorPreview() {
    EditIndicator(
        indicators = mapOf(),
        letter = 'S',
        packageName = "com.android.google.apps.messaging",
        activeList = PreviewPackageList,
        allList = listOf()
    ) { }
}

@Preview
@Composable
fun EditIndicatorEmpty() {
    EditIndicator(
        indicators = mapOf(),
        letter = ' ',
        packageName = "",
        activeList = PreviewPackageList,
        allList = listOf()
    ) { }
}
