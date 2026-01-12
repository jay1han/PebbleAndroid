package name.jayhan.pebble

import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.createBitmap

@Composable
fun SelectPackage(
    activeList: List<String>,
    fullList: List<String>,
    onClose: () -> Unit,
    onSelect: (String) -> Unit
) {
    var showAll by remember { mutableStateOf(false) }
    val listShown = if (showAll) fullList else activeList

    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = {
            if (showAll) showAll = false
            onClose()
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .verticalScroll(scrollState),
            ) {
                Text(
                    text = stringResource(R.string.select_package),
                    fontSize = AppConstants.titleSize,
                    modifier = Modifier.padding(10.dp)
                )
                if (listShown.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_active),
                        fontSize = AppConstants.textSize,
                        modifier = Modifier.fillMaxWidth().padding(10.dp)
                    )
                } else {
                    for (packageName in listShown) {
                        ListItem(
                            modifier = Modifier
                                .padding(0.dp)
                                .fillMaxWidth(),
                            leadingContent = {
                                val icon = getApplicationIcon(LocalContext.current, packageName)
                                if (icon != null)
                                    Image(
                                        bitmap = icon,
                                        contentDescription = packageName,
                                        modifier = Modifier.fillMaxWidth(.15f).padding(0.dp)
                                    )
                            },
                            headlineContent = {
                                TextButton(
                                    modifier = Modifier.weight(1f).padding(0.dp),
                                    onClick = {
                                        onSelect(packageName)
                                        onClose()
                                    },
                                ) {
                                    Text(
                                        text = packageName,
                                        fontSize = AppConstants.smallSize,
                                        modifier = Modifier.fillMaxWidth().padding(0.dp),
                                    )
                                }
                            }
                        )
                    }
                }
                Button(
                    modifier = Modifier.padding(10.dp),
                    onClick = { showAll = true }
                ) {
                    Text(
                        text = stringResource(R.string.list_all),
                        fontSize = AppConstants.textSize
                    )
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
    } catch (e: PackageManager.NameNotFoundException) {
        return null
    }

    val bitmap = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap.asImageBitmap()
}

fun getApplicationName(
    context: Context,
    packageName: String
): String {
    if (packageName.isEmpty()) return ""

    try {
        val info = context.packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        val name = context.packageManager.getApplicationLabel(info)
        return name.toString()
    } catch (e: PackageManager.NameNotFoundException) {
        return ""
    }
}

fun acceptLetter(input: String): Char {
    if (input.isEmpty()) return ' '

    val letter = input.removePrefix(" ").removeSuffix(" ").last()
    if (letter.code >= '!'.code && letter.code <= '~'.code) return letter

    return ' '
}

@Composable
fun EditPackage(
    map: Map<String, Char>,
    letter: Char,
    packageName: String,
    activeList: List<String>,
    fullList: List<String>,
    onClose: () -> Unit
) {
    var newLetter by remember { mutableStateOf(letter) }
    var newPackage by remember { mutableStateOf(packageName) }
    var showList by remember { mutableStateOf(false) }
    var editingPackage by remember { mutableStateOf(false) }

    if (showList) {
        SelectPackage(
            activeList = activeList.distinct().filter { !map.containsKey(it) },
            fullList = fullList.distinct().filter { !map.containsKey(it) },
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
                                    .background(AppConstants.colorBack)
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

@Composable
fun MainPackageList(
    activeList: List<String>,
    allList: List<String>
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editLetter by remember { mutableStateOf(' ') }
    var editPackageName by remember { mutableStateOf("") }
    val packageMap by Notifications.mapFlow.collectAsState(emptyMap())

    if (showEditDialog) {
        EditPackage(
            map = packageMap,
            letter = editLetter,
            packageName = editPackageName,
            activeList = activeList,
            fullList = allList
        ) {
            showEditDialog = false
        }
    }

    Column {
        Section(stringResource(R.string.packages))
        for (item in packageMap.toSortedMap()) {
            PackageLine(
                letter = item.value,
                packageName = item.key,
                onEdit = {
                    editLetter = item.value
                    editPackageName = item.key
                    showEditDialog = true
                })
        }

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Button(
                onClick = {
                    Notifications.reset()
                },
            ) {
                Text(
                    text = stringResource(R.string.reset),
                    fontSize = AppConstants.textSize
                )
            }
            Button(
                onClick = {
                    editLetter = ' '
                    editPackageName = ""
                    showEditDialog = true
                },
            ) {
                Text(
                    text = stringResource(R.string.add),
                    fontSize = AppConstants.textSize
                )
            }
        }
    }
}

@Composable
fun PackageLine(
    letter: Char,
    packageName: String,
    onEdit: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 4.dp)
    ) {
        Text(
            text = letter.toString(),
            fontSize = AppConstants.titleSize,
            color = AppConstants.colorText,
            modifier = Modifier
                .fillMaxWidth(.1f)
                .background(AppConstants.colorBack)
                .padding(horizontal = 16.dp),
            textAlign = TextAlign.Center,
        )

        Box(
            modifier = Modifier.fillMaxWidth(.1f)
        ) {
            val icon = getApplicationIcon(LocalContext.current, packageName)
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = packageName,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Text(
            text = packageName,
            fontSize = AppConstants.textSize,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .weight(1f)
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

val PreviewPackageList = listOf(
    "com.android.google.apps.messaging",
    "com.android.google.apps.messaging",
    "com.whatsapp"
)

val PreviewMap = mapOf(
    'S' to "com.android.google.apps.messaging",
    'W' to "com.whatsapp"
)

@Preview
@Composable
fun SelectPackagePreview() {
    SelectPackage(PreviewPackageList, listOf(), {}) { }
}

@Preview
@Composable
fun EditPackagePreview() {
    EditPackage(
        map = mapOf(),
        letter = 'S',
        packageName = "com.android.google.apps.messaging",
        activeList = PreviewPackageList,
        fullList = listOf()
    ) { }
}

@Preview
@Composable
fun EditPackageEmpty() {
    EditPackage(
        map = mapOf(),
        letter = ' ',
        packageName = "",
        activeList = PreviewPackageList,
        fullList = listOf()
    ) { }
}

@Preview
@Composable
fun MainPackageListPreview() {
    MainPackageList(PreviewPackageList, listOf())
}

@Preview
@Composable
fun SelectPackageEmpty() {
    SelectPackage(listOf(), listOf(), {}) { }
}

@Preview
@Composable
fun PackageLinePreview() {
    PackageLine(
        'S',
        "com.google.android.apps.messaging"
    ) { }
}