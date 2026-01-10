package name.jayhan.pebble

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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
    ActivityResultCallback<Map<String, Boolean>> {
    override fun onActivityResult(
        result: Map<String, Boolean>
    ) {
        for (permission in result) {
            if (permission.value)
                Permissions.update(permission.key)
        }

        if (Permissions.allGranted())
            onAllGranted()
    }
}

object Permissions {
    private lateinit var context: Context
    private lateinit var mainActivity: ComponentActivity
    val list = mutableListOf<SinglePermission>()

    fun start(
        context: Context,
        mainActivity: MainActivity,
        onAllGranted: () -> Unit
    ) {
        this.context = context
        this.mainActivity = mainActivity

        for (name in permissionsList) {
            list.add(SinglePermission(this.context, name))
        }

        if (allGranted())
            onAllGranted()
    }

    fun requestAll(
        onAllGranted: (() -> Unit)
    ) {
        while (!allGranted()) {
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

            val permissionsContract = ActivityResultContracts.RequestMultiplePermissions()
            val permissionsLauncher =
                mainActivity.registerForActivityResult(permissionsContract, PermissionsCallback(onAllGranted))
            permissionsLauncher.launch(request.toTypedArray())
        }
    }

    fun allGranted(): Boolean {
        for (singlePermission in list) {
            if (!singlePermission.granted) return false
        }
        return true
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
    }
}

@Composable
fun UiPermissions(
    modifier: Modifier,
    onAllGranted: () -> Unit
) {
    Column {
        Text(
            "Please grant permissions"
        )
        Button (
            onClick = {
                Permissions.requestAll(onAllGranted)
            }
        ) {
            Text(
                "Retry"
            )
        }
    }
}
