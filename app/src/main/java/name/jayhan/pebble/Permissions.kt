package name.jayhan.pebble

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.MutableStateFlow

const val NOTIFICATION_LISTENER = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
const val FOREGROUND_SERVICE = "android.permission.FOREGROUND_SERVICE"
const val POST_NOTIFICATION = "android.permission.POST_NOTIFICATIONS"
const val PHONE_STATE = "android.permission.READ_PHONE_STATE"
const val NEARBY_SERVICES = "android.permission.BLUETOOTH_CONNECT"
const val BACKGROUND_LOCATION = "android.permission.ACCESS_BACKGROUND_LOCATION"

val permissionsList = listOf(
    NOTIFICATION_LISTENER,
    FOREGROUND_SERVICE,
    POST_NOTIFICATION,
    PHONE_STATE,
    NEARBY_SERVICES,
    BACKGROUND_LOCATION
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
    var allGranted = false
    val grantFlow = MutableStateFlow(allGranted)
    private lateinit var mainActivity: ComponentActivity
    val list = mutableListOf<SinglePermission>()
    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>

    fun start(
        context: Context,
        mainActivity: MainActivity,
        onAllGranted: () -> Unit
    ) {
        this.mainActivity = mainActivity

        for (name in permissionsList) {
            list.add(SinglePermission(context, name))
        }

        val permissionsContract = ActivityResultContracts.RequestMultiplePermissions()
        permissionsLauncher =
            this.mainActivity.registerForActivityResult(
                permissionsContract,
                PermissionsCallback(onAllGranted)
            )

        updateAll()
        if (allGranted) {
            onAllGranted()
        }
    }

    fun requestAll() {
        val request = mutableListOf<String>()
        for (singlePermission in list) {
            if (!singlePermission.granted) {
                when (singlePermission.name) {
                    NOTIFICATION_LISTENER -> {
                        val settingsIntent =
                            Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                        mainActivity.startActivity(settingsIntent)
                    }

                    else -> {
                        request.add(singlePermission.name)
                    }
                }
            }
        }

        if (request.isNotEmpty()) {
            permissionsLauncher.launch(request.toTypedArray())
        }
    }

    fun updateAll() {
        for (singlePermission in list) {
            if (!singlePermission.granted) {
                allGranted = false
                grantFlow.value = false
                return
            }
        }
        allGranted = true
        grantFlow.value = true
    }

    fun update(
        name: String
    ) {
        for (singlePermission in list) {
            if (singlePermission.name == name) {
                singlePermission.update()
                break
            }
        }
        updateAll()
    }
}

@Composable
fun UiPermissions(
    modifier: Modifier,
) {
    Column (
        modifier = modifier
    ){
        Text(
            "Please grant permissions",
            fontSize = AppConstants.textSize

        )
        Button (
            onClick = {
                Permissions.requestAll()
            }
        ) {
            Text(
                "Retry",
                fontSize = AppConstants.textSize
            )
        }
    }
}
