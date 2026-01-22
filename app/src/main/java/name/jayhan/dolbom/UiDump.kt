package name.jayhan.dolbom

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun DumpDialog(
    dump: List<NotificationDump>,
    onClose: () -> Unit
) {
    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = {
            onClose()
        }
    ) {
        Card(
            modifier = Modifier.fillMaxSize().padding(0.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(0.dp)
            ) {
                item { HorizontalDivider(thickness = 1.dp) }
                items(
                    items = dump,
                ) { notificationDump ->
                    Text(notificationDump.packageName)
                    Text(notificationDump.channelId)
                    notificationDump.extraMap.forEach { (filterType, extrasMap) ->
                        Text(text = filterType.name)
                        extrasMap.forEach { (name, value) ->
                            Text(text = "$name: $value")
                        }
                    }
                    HorizontalDivider(thickness = 1.dp)
                }
            }
        }
    }
}

val PreviewDump = listOf(
    NotificationDump(
        "com.google",
        "channelId",
        mapOf(
            FilterType.Title to mapOf(
                "Title" to "Hello"
            ),
            FilterType.People to mapOf(
                "People" to "You"
            ),
            FilterType.Text to mapOf(
                "Text" to "World"
            ),
        )
    )
)

@Preview
@Composable
fun DumpDialogPreview() {
    DumpDialog(PreviewDump) {}
}