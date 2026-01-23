package name.jayhan.dolbom

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                items(
                    items = dump,
                ) { notificationDump ->
                    Text(
                        notificationDump.packageName,
                        fontSize = Const.textSize,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        notificationDump.channelId,
                        fontSize = Const.smallSize,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    notificationDump.extraMap.forEach { (filterType, extrasMap) ->
                        Text(
                            text = filterType.name,
                            fontSize = Const.smallSize,
                        )
                        extrasMap.forEach { (name, value) ->
                            if (value.isNotEmpty())
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                            append("$name : ")
                                        }
                                        append(value)
                                    },
                                    lineHeight = Const.subSize * 1.2,
                                    fontSize = Const.subSize,
                                )
                        }
                    }
                    Spacer(modifier = Modifier.height(5.dp))
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
            FilterType.Subtitle to mapOf(
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