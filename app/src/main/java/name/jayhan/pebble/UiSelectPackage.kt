package name.jayhan.pebble

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun SelectPackage(
    activeList: List<String>,
    allList: List<String>,
    onClose: () -> Unit,
    onSelect: (String) -> Unit
) {
    var showAllState by remember { mutableStateOf(false) }
    val showAllActual = showAllState || activeList.isEmpty()
    val listShown = if (showAllActual) allList else activeList

    Dialog(
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        ),
        onDismissRequest = {
            if (showAllState) showAllState = false
            onClose()
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(0.dp)
        ) {
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                if (!showAllActual) {
                    item {
                        Text(
                            text = stringResource(R.string.select_package),
                            fontSize = AppConstants.titleSize,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                if (listShown.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.no_active),
                            fontSize = AppConstants.textSize,
                            modifier = Modifier.fillMaxWidth().padding(10.dp)
                        )
                    }
                } else {
                    items(
                        items = listShown,
                    ) { packageName ->
                        ListItem(
                            modifier = Modifier
                                .clickable {
                                    onSelect(packageName)
                                    onClose()
                                }
                                .padding(0.dp)
                                .fillMaxWidth(),
                            leadingContent = {
                                val appIcon = getApplicationIcon(LocalContext.current, packageName)
                                if (appIcon != null) {
                                    Image(
                                        bitmap = appIcon,
                                        contentDescription = packageName,
                                        modifier = Modifier.fillMaxWidth(.15f).padding(0.dp)
                                    )
                                }
                            },
                            headlineContent = {
                                val appName = Notifications.getAppName(packageName)
                                if (appName != "") {
                                    Text(
                                        text = appName,
                                        fontSize = AppConstants.smallSize
                                    )
                                }
                            },
                            supportingContent = {
                                Text(
                                    text = packageName,
                                    fontSize = AppConstants.subSize,
                                    modifier = Modifier.fillMaxWidth().padding(0.dp),
                                )
                            }
                        )
                    }
                }
                if (!showAllActual) {
                    item {
                        Button(
                            modifier = Modifier.padding(10.dp),
                            onClick = { showAllState = true }
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
    }
}

@Preview
@Composable
fun SelectPackagePreview() {
    SelectPackage(PreviewActiveList, listOf(), {}) { }
}

@Preview
@Composable
fun SelectPackageNoActive() {
    SelectPackage(listOf(), PreviewActiveList, {}) { }
}
