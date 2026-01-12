package name.jayhan.pebble

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.flow.MutableStateFlow

const val ENABLED_LISTENERS = "enabled_notification_listeners"

const val ACTION_LISTENER = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
const val NOTIFICATION_LISTENER = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
const val FOREGROUND_SERVICE = "android.permission.FOREGROUND_SERVICE"
const val POST_NOTIFICATION = "android.permission.POST_NOTIFICATIONS"
const val PHONE_STATE = "android.permission.READ_PHONE_STATE"
const val NEARBY_SERVICES = "android.permission.BLUETOOTH_CONNECT"
const val BLUETOOTH_CONNECT = "android.permission.BLUETOOTH_CONNECT"
const val FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"
const val BACKGROUND_LOCATION = "android.permission.ACCESS_BACKGROUND_LOCATION"
const val QUERY_ALL_PACKAGES = "android.permission.QUERY_ALL_PACKAGES"

val PermissionsList = mapOf(
    NOTIFICATION_LISTENER to Pair(
        R.string.notification_listener,
        R.string.notification_listener_2),
    FOREGROUND_SERVICE to Pair(
        R.string.foreground_service,
        R.string.foreground_services_2),
    POST_NOTIFICATION to Pair(
        R.string.post_notification,
        R.string.post_notification_2),
    PHONE_STATE to Pair(
        R.string.phone_state,
        R.string.phone_state_2),
    NEARBY_SERVICES to Pair(
        R.string.nearby_service,
        R.string.nearby_service_2),
    BLUETOOTH_CONNECT to Pair(
        R.string.bluetooth_connect,
        R.string.bluetooth_connect_2),
    FINE_LOCATION to Pair(
        R.string.fine_location,
        R.string.fine_location_2),
    BACKGROUND_LOCATION to Pair(
        R.string.background_location,
        R.string.background_location_2),
    QUERY_ALL_PACKAGES to Pair(
        R.string.query_all_packages,
        R.string.query_all_packages_2),
)

class SinglePermission(
    private val context: Context,
    val name: String
) {
    var granted = update()

    fun update(): Boolean {
        granted = when (name) {
            NOTIFICATION_LISTENER ->
                Settings.Secure.getString(
                    context.contentResolver,
                    ENABLED_LISTENERS
                )
                    .contains(context.packageName)

            else ->
                context.checkSelfPermission(name) ==
                        PackageManager.PERMISSION_GRANTED

        }
        return granted
    }
}

class PermissionsCallback(
    private val onAllGranted: () -> Unit
):
    ActivityResultCallback<Map<String, Boolean>>
{
    override fun onActivityResult(
        result: Map<String, Boolean>
    ) {
        if (result.isNotEmpty()) {
            for (permission in result) {
                if (permission.value)
                    Permissions.update(permission.key)
            }

            if (Permissions.allGranted)
                onAllGranted()
        }
    }
}

object Permissions
{
    private lateinit var mainActivity: ComponentActivity
    var allGranted = false
    val grantFlow = MutableStateFlow(allGranted)
    val missingFlow = MutableStateFlow(listOf<String>())
    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var permissionList: List<SinglePermission>

    fun start(
        context: Context,
        mainActivity: MainActivity,
        onAllGranted: () -> Unit
    ) {
        this.mainActivity = mainActivity
        permissionList = PermissionsList.map { SinglePermission(context, it.key) }

        val permissionsContract = ActivityResultContracts.RequestMultiplePermissions()
        permissionsLauncher =
            this.mainActivity.registerForActivityResult(
                permissionsContract,
                PermissionsCallback(onAllGranted)
            )

        collect()
        if (allGranted) {
            onAllGranted()
        }
    }

    fun request(
        permissionName: String
    ): Boolean {
        when (permissionName) {
            NOTIFICATION_LISTENER -> {
                val settingsIntent =
                    Intent(ACTION_LISTENER)
                mainActivity.startActivity(settingsIntent)
                return false
            }

            else -> {
                if (mainActivity.shouldShowRequestPermissionRationale(permissionName))
                    return true
            }
        }
        permissionsLauncher.launch(arrayOf(permissionName))
        return false
    }

    fun collect() {
        val missingList = permissionList
            .filter { !it.granted }
            .map { it.name }

        allGranted = missingList.isEmpty()
        grantFlow.value = allGranted
        missingFlow.value = missingList
    }

    fun update(
        name: String
    ) {
        val permission = permissionList.first { it.name == name }
        if (!permission.granted) {
            permission.update()
            collect()
        }
    }
}

@Composable
fun UiPermissions(
    missingList: List<String>,
    modifier: Modifier = Modifier
) {
    var showRationale by remember { mutableStateOf("") }

    if (showRationale != "") {
        Rationale(
            permission = showRationale,
            onClick = {
                Permissions.request(showRationale)
            }) {
            showRationale = ""
        }
    } else {
        val scrollState = rememberScrollState()
        Column(
            modifier = modifier
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Required permissions"
            )

            for (permission in missingList) {
                ListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    headlineContent = {
                        Text(
                            text = permission.removePrefix("android.permission."),
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = AppConstants.textSize
                        )
                    },
                    supportingContent = {
                        Text(
                            text = stringResource(PermissionsList[permission]!!.first),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            fontSize = AppConstants.smallSize,
                        )
                    },
                    trailingContent = {
                        IconButton(
                            onClick = {
                                if (Permissions.request(permission))
                                    showRationale = permission
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.outline_chevron_forward_24),
                                contentDescription = "Go"
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun Rationale(
    permission: String,
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
                    text = permission
                )
                Text(
                    text = stringResource(PermissionsList[permission]!!.first)
                )
                Text(
                    text = stringResource(PermissionsList[permission]!!.second)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            onClick()
                            onClose()
                        }
                    ) {
                        Text(
                            text = "Accept"
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
    val missingList = PermissionsList.map { it.key }

    UiPermissions(
        missingList,
        Modifier
    )
}

@Preview
@Composable
fun RationalePreview() {
    Rationale(
        permission = POST_NOTIFICATION,
        onClick = {}
    ) {}
}
