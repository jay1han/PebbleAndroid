package name.jayhan.pebble

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.flow.MutableStateFlow

const val SETTINGS_ENABLED_LISTENERS = "enabled_notification_listeners"
const val ACTION_LISTENER_SETTING = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"

const val FOREGROUND_SERVICE = "android.permission.FOREGROUND_SERVICE"
const val CONNECTED_DEVICE = "android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE"
const val POST_NOTIFICATION = "android.permission.POST_NOTIFICATIONS"
const val BLUETOOTH_CONNECT = "android.permission.BLUETOOTH_CONNECT"
const val NETWORK_STATE = "android.permission.ACCESS_NETWORK_STATE"
const val WIFI_STATE = "android.permission.ACCESS_WIFI_STATE"
const val FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"
const val BACKGROUND_LOCATION = "android.permission.ACCESS_BACKGROUND_LOCATION"
const val PHONE_STATE = "android.permission.READ_PHONE_STATE"
const val QUERY_ALL_PACKAGES = "android.permission.QUERY_ALL_PACKAGES"
const val RECEIVE_BOOT_COMPLETED = "android.permission.RECEIVE_BOOT_COMPLETED"
const val NOTIFICATION_POLICY = "android.permission.ACCESS_NOTIFICATION_POLICY"
const val NOTIFICATION_LISTENER = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
const val AUDIO_SETTINGS = "android.permission.MODIFY_AUDIO_SETTINGS"

val AllPermissionGroups = listOf(
    PermissionGroup(
        R.string.pg_audio_settings,
        listOf(AUDIO_SETTINGS),
        R.string.audio_settings,
        R.string.audio_settings_2),
    PermissionGroup(
        R.string.pg_boot_completed,
        listOf(RECEIVE_BOOT_COMPLETED),
        R.string.boot_completed,
        R.string.boot_completed_2),
    PermissionGroup(
        R.string.pg_read_notifications,
        listOf(NOTIFICATION_LISTENER),
        R.string.read_notifications,
        R.string.read_notifications_2),
    PermissionGroup(
        R.string.pg_foreground_service,
        listOf(FOREGROUND_SERVICE, CONNECTED_DEVICE),
        R.string.foreground_service,
        R.string.foreground_services_2),
    PermissionGroup(
        R.string.pg_post_notification,
        listOf(POST_NOTIFICATION),
        R.string.post_notification,
        R.string.post_notification_2),
    PermissionGroup(
        R.string.pg_modem_state,
        listOf(NETWORK_STATE, WIFI_STATE, PHONE_STATE),
        R.string.modem_state,
        R.string.modem_state_2),
    PermissionGroup(
        R.string.pg_nearby_services,
        listOf(BLUETOOTH_CONNECT),
        R.string.nearby_service,
        R.string.nearby_service_2),
    PermissionGroup(
        R.string.pg_fine_location,
        listOf(FINE_LOCATION),
        R.string.fine_location,
        R.string.fine_location_2),
    PermissionGroup(
        R.string.pg_background_location,
        listOf(BACKGROUND_LOCATION),
        R.string.background_location,
        R.string.background_location_2),
    PermissionGroup(
        R.string.pg_query_apps,
        listOf(QUERY_ALL_PACKAGES),
        R.string.query_apps,
        R.string.query_apps_2),
    PermissionGroup(
        R.string.pg_zen_rule,
        listOf(NOTIFICATION_POLICY),
        R.string.zen_rule,
        R.string.zen_rule_2),
)

class PermissionGroup(
    val title: Int,
    private val listOfNames: List<String>,
    val description: Int,
    val rationale: Int
) {
    private lateinit var listOfSingles: List<SinglePermission>
    var granted = false

    fun init(
        context: Context
    ) {
        listOfSingles = listOfNames.map { SinglePermission(context, it) }
    }

    fun request() {
        listOfSingles
            .filter { !it.granted }
            .forEach {
                Permissions.requestSingle(it)
            }
    }

    fun update(): Boolean {
        var allGranted = true

        listOfSingles.forEach {
            allGranted = allGranted and it.update()
        }
        granted = allGranted
        return allGranted
    }

    fun findSingle(permission: String): SinglePermission? {
        if (!this::listOfSingles.isInitialized) return null
        val singlePermission = listOfSingles.find {
            it.permission == permission
        }
        return singlePermission
    }
}

class SinglePermission(
    private val context: Context,
    val permission: String
) {
    var granted = update()

    fun update(): Boolean {
        granted = when (permission) {
            NOTIFICATION_LISTENER ->
                Settings.Secure.getString(
                    context.contentResolver,
                    SETTINGS_ENABLED_LISTENERS
                )
                    .contains(context.packageName)
            
            else ->
                context.checkSelfPermission(permission) ==
                        PackageManager.PERMISSION_GRANTED

        }
        return granted
    }

    fun request(
        mainActivity: ComponentActivity,
        permissionsLauncher: ActivityResultLauncher<Array<String>>
    ) {
        when (permission) {
            NOTIFICATION_LISTENER -> {
                mainActivity.startActivity(
                    Intent(ACTION_LISTENER_SETTING))
                return
            }
            
            else ->
                mainActivity.shouldShowRequestPermissionRationale(permission)
        }
        permissionsLauncher.launch(arrayOf(permission))
    }
}

class PermissionsCallback():
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
            Permissions.updateAll()
        }
    }
}

object Permissions
{
    private lateinit var mainActivity: ComponentActivity
    var allGranted = false
    var allInit = false
    val grantFlow = MutableStateFlow(allGranted)
    val initFlow = MutableStateFlow(false)
    val missingFlow = MutableStateFlow(listOf<PermissionGroup>())
    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>

    fun findSinglePermission(permission: String): SinglePermission? {
        AllPermissionGroups.forEach {
            val singlePermission = it.findSingle(permission)
            if (singlePermission != null) return singlePermission
        }
        return null
    }

    fun initService(context: Context) {
        AllPermissionGroups.forEach { it.init(context) }
        allInit = true
        if (this::mainActivity.isInitialized) {
            initFlow.value = true
        }
        updateAll()
        Log.v(Const.TAG, "Permissions allGranted=$allGranted")
    }

    fun initActivity(
        mainActivity: MainActivity,
    ) {
        this.mainActivity = mainActivity
        val permissionsContract = ActivityResultContracts.RequestMultiplePermissions()
        permissionsLauncher =
            this.mainActivity.registerForActivityResult(
                permissionsContract,
                PermissionsCallback()
            )
        if (allInit) initFlow.value = true
    }

    fun requestGroup(
        permissionGroup: PermissionGroup
    ) {
        permissionGroup.request()
    }

    fun requestSingle(
        singlePermission: SinglePermission
    ) {
        singlePermission.request(mainActivity, permissionsLauncher)
    }

    fun collectMissing() {
        val missingList = AllPermissionGroups
            .filter { !it.granted }

        allGranted = missingList.isEmpty()
        grantFlow.value = allGranted
        missingFlow.value = missingList
    }

    fun update(
        name: String
    ) {
        val singlePermission = findSinglePermission(name)
        if (singlePermission != null) {
            singlePermission.update()
            collectMissing()
        }
    }

    fun restartService(
        context: Context
    ) {
        if (allGranted) {
            val intent = Intent(context, PebbleService::class.java)
            context.startForegroundService(intent)
        }
    }

    fun updateAll() {
        AllPermissionGroups.forEach {
            it.update()
        }
        collectMissing()
    }
}
