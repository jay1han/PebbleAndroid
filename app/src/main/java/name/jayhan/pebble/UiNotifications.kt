package name.jayhan.pebble

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun SelectPackage(
    packageList: List<String>,
    onClose: () -> Unit,
    onSelect: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onClose
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(10.dp)
                    .verticalScroll(scrollState),
            ) {
                Text(
                    text = stringResource(R.string.select_package),
                    fontSize = AppConstants.titleSize,
                    modifier = Modifier.padding(10.dp)
                )
                if (packageList.isNotEmpty()) {
                    for (packageName in packageList) {
                        ListItem(
                            modifier = Modifier.padding(0.dp),
                            headlineContent = {
                                TextButton(
                                    modifier = Modifier.padding(0.dp),
                                    onClick = {
                                        onSelect(packageName)
                                        onClose()
                                    },
                                ) {
                                    Text(
                                        text = packageName,
                                        fontSize = AppConstants.textSize,
                                        modifier = Modifier.padding(0.dp),
                                    )
                                }
                            }
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.no_active),
                        fontSize = AppConstants.textSize,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EditPackage(
    mapper: Mapper,
    letter: Char,
    packageName: String,
    packageList: List<String>,
    onClose: () -> Unit
) {
    var newLetter by remember { mutableStateOf(letter) }
    var newPackage by remember { mutableStateOf(packageName) }
    var showList by remember { mutableStateOf(false) }
    var icon: ImageBitmap? = null

    if (packageList != PreviewPackageList && newPackage.isNotEmpty()) {
        val drawable = LocalContext.current.packageManager
            .getApplicationIcon(newPackage)
        if (drawable != null) {
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            icon = bitmap.asImageBitmap()
        }
    }

    if (showList) {
        SelectPackage(
            packageList,
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
                                onValueChange = { newLetter = if (it.isNotEmpty()) it.uppercase().last() else ' ' },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = AppConstants.titleSize,
                                    color = AppConstants.colorText,
                                    textAlign = TextAlign.Center,
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

                    Text(
                        text = newPackage.ifEmpty { stringResource(R.string.no_package) },
                        fontSize = AppConstants.textSize,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 0.dp,
                                vertical = 10.dp
                            ),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                if (newLetter != ' ' && newPackage.isNotEmpty())
                                    mapper.register(newLetter, newPackage)
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
                                mapper.register(' ', newPackage)
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
fun PackageList(
    map: Map<Char, String>,
    mapper: Mapper,
    packageList: List<String>
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editLetter by remember { mutableStateOf(' ') }
    var editPackageName by remember { mutableStateOf("") }

    if (showEditDialog) {
        EditPackage(
            mapper = mapper,
            letter = editLetter,
            packageName = editPackageName,
            packageList = packageList,
        ) {
            showEditDialog = false
        }
    }

    Column {
        Section(stringResource(R.string.packages))
        for (item in map.toSortedMap()) {
            PackageLine(
                item.key,
                item.value,
                onEdit = {
                    editLetter = item.key
                    editPackageName = item.value
                    showEditDialog = true
                })
        }
        if (map.size < 9) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        mapper.reset()
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
}

@Composable
fun PackageLine(
    letter: Char,
    packageName: String,
    onEdit: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(4.dp)
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
        Text(
            text = packageName,
            fontSize = AppConstants.textSize,
            modifier = Modifier
                .fillMaxWidth(.9f)
                .padding(horizontal = 8.dp)
        )
        FilledIconButton(
            onClick = onEdit
        ) {
            Icon(
                painter = painterResource(R.drawable.outline_edit_24),
                contentDescription = stringResource(R.string.edit),
            )
        }

    }
}

@Preview
@Composable
fun SelectPackagePreview() {
    SelectPackage(PreviewPackageList, {}) { }
}

@Preview
@Composable
fun EditPackagePreview() {
    val mapper = Mapper(LocalContext.current)
    EditPackage(
        mapper = mapper,
        letter ='S',
        packageName = "com.android.google.apps.messaging",
        packageList = PreviewPackageList
    ) { }
}

@Preview
@Composable
fun PackageListPreview() {
    val mapper = Mapper(LocalContext.current)
    PackageList(PreviewMap, mapper, PreviewPackageList)
}

@Preview
@Composable
fun SelectPackageEmpty() {
    SelectPackage(listOf(), {}) { }
}
