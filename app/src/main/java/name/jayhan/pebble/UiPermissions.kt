package name.jayhan.pebble

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import name.jayhan.pebble.ui.theme.PebbleTheme

@Composable
fun PermissionsScaffold(
    context: Context
) {
    val missingList by Permissions.missingFlow.collectAsState(listOf())
    var permissionHelp by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            PermissionsTopBar { permissionHelp = true }
        },
    ) { innerPadding ->
        if (permissionHelp) {
            PermissionHelp(
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            ) { permissionHelp = false }
        } else {
            UiPermissions(
                missingList,
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsTopBar(
    onHelp: () -> Unit
) {
    TopAppBar(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onHelp() },
        navigationIcon = {
            Image(
                painterResource(R.drawable.navicon),
                contentDescription = "Logo",
                modifier = Modifier
                    .padding(4.dp)
                    .height(40.dp),
            )
        },
        title = {
            Text(
                text = "Permissions",
                fontSize = AppConst.titleSize
            )
        },
        actions = {
                Text(
                    text = "?",
                    fontSize = AppConst.titleSize,
                )
        }
    )
}

@Composable
fun PermissionHelp(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    BackHandler(true, onBack)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp)
    ) {
        Text(
            text = stringResource(R.string.permissions_help),
            fontSize = AppConst.textSize,
            lineHeight = AppConst.titleSize,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.padding(10.dp))

        Button(onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(
                text = "OK",
                fontSize = AppConst.titleSize,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun UiPermissions(
    missingList: List<PermissionGroup>,
    modifier: Modifier = Modifier
) {
    var showDetails by remember { mutableStateOf<PermissionGroup?>(null) }

    if (showDetails != null) {
        Rationale(
            permissionGroup = showDetails!!,
            onClick = {
                Permissions.requestGroup(showDetails!!)
            }) {
            showDetails = null
        }
    } else {
        val scrollState = rememberScrollState()
        Column(
            modifier = modifier
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = stringResource(R.string.pg_title),
                fontSize = AppConst.titleSize
            )

            for (permissionGroup in missingList) {
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showDetails = permissionGroup
                        },
                    headlineContent = {
                        Text(
                            text = stringResource(permissionGroup.title),
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = AppConst.textSize
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(permissionGroup.description),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            fontSize = AppConst.smallSize,
                        )
                    },
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.outline_chevron_forward_24),
                            contentDescription = "Go"
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun Rationale(
    permissionGroup: PermissionGroup,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp)
            ) {
                Text(
                    text = stringResource(permissionGroup.title),
                    fontSize = AppConst.titleSize,
                    modifier = Modifier.padding(10.dp)
                )
                Text(
                    text = stringResource(permissionGroup.description),
                    fontSize = AppConst.textSize,
                    modifier = Modifier.padding(8.dp)
                )
                Text(
                    text = stringResource(permissionGroup.rationale),
                    fontSize = AppConst.smallSize,
                    modifier = Modifier.padding(4.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            onClick()
                            onClose()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.accept),
                            fontSize = AppConst.textSize
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun UiPermissionsPreview() {
    PebbleTheme {
        UiPermissions(
            AllPermissionGroups,
            Modifier
        )
    }
}

@Preview
@Composable
fun RationalePreview() {
    PebbleTheme {
        Rationale(
            AllPermissionGroups[3],
            onClick = {}
        ) {}
    }
}

@Preview
@Composable
fun PermissionsTopBarPreview() {
    PebbleTheme {
        PermissionsTopBar {  }
    }
}

@Preview
@Composable
fun PermissionHelpPreview() {
    PebbleTheme {
        PermissionHelp(Modifier) { }
    }
}
