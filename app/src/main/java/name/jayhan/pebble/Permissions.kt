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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow

const val NOTIFICATION_LISTENER = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
const val FOREGROUND_SERVICE = "android.permission.FOREGROUND_SERVICE"
const val POST_NOTIFICATION = "android.permission.POST_NOTIFICATIONS"
const val PHONE_STATE = "android.permission.READ_PHONE_STATE"
const val NEARBY_SERVICES = "android.permission.BLUETOOTH_CONNECT"
const val FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"
const val BACKGROUND_LOCATION = "android.permission.ACCESS_BACKGROUND_LOCATION"

val permissionsList = mapOf(
    NOTIFICATION_LISTENER to R.string.notification_listener,
    FOREGROUND_SERVICE to R.string.foreground_service,
    POST_NOTIFICATION to R.string.post_notification,
    PHONE_STATE to R.string.phone_state,
    NEARBY_SERVICES to R.string.nearby_service,
    FINE_LOCATION to R.string.fine_location,
    BACKGROUND_LOCATION to R.string.background_location
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
                    "enabled_notification_listeners"
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
    val list = mutableListOf<SinglePermission>()
    var allGranted = false
    val grantFlow = MutableStateFlow(allGranted)
    val missingFlow = MutableStateFlow(listOf<String>())
    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>

    fun start(
        context: Context,
        mainActivity: MainActivity,
        onAllGranted: () -> Unit
    ) {
        this.mainActivity = mainActivity

        for (pair in permissionsList) {
            list.add(SinglePermission(context, pair.key))
        }

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

    fun request() {
        val request = mutableListOf<String>()
        for (permission in list) {
            if (!permission.granted) {
                when (permission.name) {
                    NOTIFICATION_LISTENER -> {
                        val settingsIntent =
                            Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                        mainActivity.startActivity(settingsIntent)
                        break
                    }

                    else -> {
                        mainActivity.shouldShowRequestPermissionRationale(permission.name)
                        request.add(permission.name)
                        break
                    }
                }
            }
        }

        if (request.isNotEmpty()) {
            permissionsLauncher.launch(request.toTypedArray())
        }
    }

    fun collect() {
        val missingList = mutableListOf<String>()

        for (permission in list) {
            if (!permission.granted) {
                missingList.add(permission.name)
            }
        }

        allGranted = missingList.isEmpty()
        grantFlow.value = allGranted
        missingFlow.value = missingList
    }

    fun update(
        name: String
    ) {
        for (permission in list) {
            if (permission.name == name) {
                if (!permission.granted) permission.update()
                break
            }
        }
        collect()
    }
}

@Composable
fun UiPermissions(
    missingList: List<String>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Top
    ) {
        Column {
            for (permission in missingList) {
                Text(
                    text = permission.removePrefix("android.permission."),
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = AppConstants.textSize
                )
                Text(
                    text = stringResource(permissionsList[permission]!!),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    fontSize = AppConstants.smallSize,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    Permissions.request()
                },
            ) {
                Text(
                    stringResource(R.string.grant),
                    fontSize = AppConstants.textSize,
                )
            }
        }
    }
}

@Preview
@Composable
fun UiPermissionsPreview() {
    val missingList = permissionsList.map { it.key }

    UiPermissions(
        missingList,
        Modifier
    )
}
