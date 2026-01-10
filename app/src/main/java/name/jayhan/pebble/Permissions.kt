package name.jayhan.pebble

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
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
        when (name) {
            NOTIFICATION_LISTENER -> {
                granted = Settings.Secure.getString(
                    context.contentResolver,
                    "enabled_notification_listeners"
                ).contains(context.packageName)
            }
            else -> {
                granted = context.checkSelfPermission(name) == PackageManager.PERMISSION_GRANTED
            }
        }
        return granted
    }
}

class PermissionCallback(
    private val name: String
): ActivityResultCallback<Boolean> {
    override fun onActivityResult(result: Boolean) {
        if (result) Permissions.grant(name)
    }
}

object Permissions {
    private lateinit var context: Context
    val list = mutableListOf<SinglePermission>()

    fun start(
        context: Context,
        mainActivity: MainActivity
    ) {
        this.context = context

        for (name in permissionsList) {
            list.add(SinglePermission(this.context, name))
        }

        // TODO: Synchronize with RequestPermissionCallback
        // TODO: Move to separate function
        while(!allGranted()) {
            for (singlePermission in list) {
                if (!singlePermission.granted) {
                    when (singlePermission.name) {
                        NOTIFICATION_LISTENER -> {
                            val settingsIntent =
                                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                            mainActivity.startActivity(settingsIntent)
                        }
                        else -> {
                            val permissionContract = ActivityResultContracts.RequestPermission()
                            val permissionLauncher =
                                mainActivity.registerForActivityResult(permissionContract, PermissionCallback(singlePermission.name))
                            permissionLauncher.launch(singlePermission.name)
                        }
                    }
                    break
                }
            }
        }
    }

    fun allGranted(): Boolean {
        for (singlePermission in list) {
            if (!singlePermission.granted) return false
        }
        return true
    }

    fun grant(
        name: String
    ) {
        for (singlePermission in list) {
            if (singlePermission.name == name) {
                singlePermission.granted = true
                break
            }
        }
    }

    fun isGranted(
        name: String
    ): Boolean {
        for (singlePermission in list) {
            if (singlePermission.name == name) {
                return singlePermission.granted
            }
        }
        return false
    }
}

@Composable
fun UiPermissions(
    modifier: Modifier
) {
    Column {
        Text(
            "Please add to notification listeners"
        )
        Button (
            onClick = {

            }
        ) {
            Text(
                "Retry"
            )
        }
    }
}
