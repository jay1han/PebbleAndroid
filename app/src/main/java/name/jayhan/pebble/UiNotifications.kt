package name.jayhan.pebble

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun ListActiveNotifications(
    onClose: () -> Unit,
    onSelect: (String) -> Unit
) {
    val notificationList by Notifications.listFlow.collectAsState(listOf())

    Dialog(
        onDismissRequest = onClose
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.select_package),
                    fontSize = AppConstants.titleSize
                )
                for (packageName in notificationList) {
                    ListItem(
                        modifier = Modifier.padding(0.dp),
                        headlineContent = {
                            TextButton(
                                onClick = {
                                    onSelect(packageName)
                                    onClose()
                                },
                            ) {
                                Text(
                                    text = packageName,
                                    fontSize = AppConstants.textSize
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EditNotificationItem(
    letter: Char,
    packageName: String,
    onClose: () -> Unit
) {
    var key by remember { mutableStateOf(letter) }
    var value by remember { mutableStateOf(packageName) }
    var showList by remember { mutableStateOf(false) }

    if (showList) {
        ListActiveNotifications(
            onClose = { showList = false }
        ) { name: String -> value = name }
    } else {
        Dialog(
            onDismissRequest = onClose,
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = key.toString(),
                        onValueChange = { key = if (it.isNotEmpty()) it.uppercase().last() else ' ' },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = AppConstants.titleSize,
                            color = AppConstants.colorText,
                        ),
                        modifier = Modifier
                            .fillMaxWidth(.2f)
                            .background(AppConstants.colorBack)
                            .padding(0.dp)
                    )

                    Text(
                        text = value,
                        fontSize = AppConstants.textSize,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = { showList = true }
                    ) {
                        Text(
                            text = stringResource(R.string.select_package),
                            fontSize = AppConstants.textSize
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            if (key != ' ' && value.isNotEmpty())
                                Mapper.register(key, value)
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
                            Mapper.register(' ', value)
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

@Composable
fun NotificationsList() {
    val map by Notifications.mapFlow.collectAsState(emptyMap())
    var showEditDialog by remember { mutableStateOf(false) }
    var editLetter by remember { mutableStateOf(' ') }
    var editPackageName by remember { mutableStateOf("") }

    if (showEditDialog) {
        EditNotificationItem(
            editLetter,
            editPackageName,
            onClose = { showEditDialog = false }
        )
    }

    Section(stringResource(R.string.notifications))
    for (item in map.toSortedMap()) {
        NotificationLine(
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
                    Mapper.reset()
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
fun NotificationLine(
    letter: Char,
    packageName: String,
    onEdit: () -> Unit
) {
    Row (
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
