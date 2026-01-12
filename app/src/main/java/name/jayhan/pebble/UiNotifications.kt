package name.jayhan.pebble

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap

@Composable
fun IndicatorList(
    activeList: List<String>,
    allList: List<String>
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editLetter by remember { mutableStateOf(' ') }
    var editPackageName by remember { mutableStateOf("") }
    val indicators by Notifications.indicatorsFlow.collectAsState(emptyMap())

    if (showEditDialog) {
        EditIndicator(
            indicators = indicators,
            letter = editLetter,
            packageName = editPackageName,
            activeList = activeList,
            allList = allList
        ) {
            showEditDialog = false
        }
    }

    Column {
        Section(stringResource(R.string.packages))

        val indicatorList = mutableListOf<Pair<String, String>>()
            .apply {
                for (item in indicators) {
                    add(Pair(item.key, Notifications.getAppName(item.key)))
                }
            }
            .apply { sortBy { it.second } }
        for (item in indicatorList.map { it.first }) {
            val letter = indicators[item]!!
            IndicatorLine(
                letter = letter,
                packageName = item,
                onEdit = {
                    editLetter = letter
                    editPackageName = item
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
fun IndicatorLine(
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
        Box(
            modifier = Modifier.fillMaxWidth(.1f).padding(4.dp)
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

        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 8.dp)
                .weight(1f)
        ) {
            val appName = Notifications.getAppName(packageName)
            if (appName != "") {
                Text(
                    text = appName,
                    fontSize = AppConstants.textSize,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                text = packageName,
                fontSize = AppConstants.subSize,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Text(
            text = letter.toString(),
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

val PreviewPackageList = listOf(
    "com.android.google.apps.messaging",
    "com.android.google.apps.messaging",
    "com.whatsapp"
)

@Preview
@Composable
fun IndicatorListPreview() {
    IndicatorList(PreviewPackageList, listOf())
}

@Preview
@Composable
fun IndicatorLinePreview() {
    IndicatorLine(
        'S',
        "com.google.android.apps.messaging"
    ) { }
}