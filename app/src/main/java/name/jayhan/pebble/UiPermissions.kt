package name.jayhan.pebble

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

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
                fontSize = AppConstants.titleSize
            )

            for (permissionGroup in missingList) {
                ListItem(
                    modifier = Modifier
                        .clickable {
                            showDetails = permissionGroup
                        }
                        .fillMaxWidth()
                        .padding(10.dp),
                    headlineContent = {
                        Text(
                            text = stringResource(permissionGroup.title),
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = AppConstants.textSize
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(permissionGroup.title),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            fontSize = AppConstants.smallSize,
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
        onDismissRequest = {}
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = stringResource(permissionGroup.title),
                    fontSize = AppConstants.titleSize,
                    modifier = Modifier.padding(10.dp)
                )
                Text(
                    text = stringResource(permissionGroup.description),
                    fontSize = AppConstants.textSize,
                    modifier = Modifier.padding(8.dp)
                )
                Text(
                    text = stringResource(permissionGroup.rationale),
                    fontSize = AppConstants.smallSize,
                    modifier = Modifier.padding(4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
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
                            fontSize = AppConstants.textSize
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
    val missingList = AllPermissionGroups

    UiPermissions(
        missingList,
        Modifier
    )
}

@Preview
@Composable
fun RationalePreview() {
    Rationale(
        AllPermissionGroups[3],
        onClick = {}
    ) {}
}
