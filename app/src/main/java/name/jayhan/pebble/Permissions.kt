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

/*
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
    <uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY" />
 */
const val FOREGROUND_SERVICE = "android.permission.FOREGROUND_SERVICE"
const val CONNECTED_DEVICE = "android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE"
const val POST_NOTIFICATION = "android.permission.POST_NOTIFICATIONS"
const val PHONE_STATE = "android.permission.READ_PHONE_STATE"
const val NEARBY_SERVICES = "android.permission.BLUETOOTH_CONNECT"
const val BLUETOOTH_CONNECT = "android.permission.BLUETOOTH_CONNECT"
const val FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"
const val BACKGROUND_LOCATION = "android.permission.ACCESS_BACKGROUND_LOCATION"
const val QUERY_ALL_PACKAGES = "android.permission.QUERY_ALL_PACKAGES"
const val NOTIFICATION_LISTENER = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"

val AllPermissionGroups = listOf(
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
        R.string.pg_read_phone_state,
        listOf(PHONE_STATE),
        R.string.phone_state,
        R.string.phone_state_2),
    PermissionGroup(
        R.string.pg_nearby_services,
        listOf(BLUETOOTH_CONNECT),
        R.string.nearby_service,
        R.string.nearby_service_2),
    PermissionGroup(
        R.string.pg_location_services,
        listOf(FINE_LOCATION),
        R.string.fine_location,
        R.string.fine_location_2),
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
        R.string.pg_query_installed_apps,
        listOf(QUERY_ALL_PACKAGES),
        R.string.query_all_packages,
        R.string.query_all_packages_2)
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
            it.name == permission
        }
        return singlePermission
    }
}

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
                    SETTINGS_ENABLED_LISTENERS
                )
                    .contains(context.packageName)

            else ->
                context.checkSelfPermission(name) ==
                        PackageManager.PERMISSION_GRANTED

        }
        return granted
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

    fun isGranted(permission: String): Boolean {
        val singlePermission = findSinglePermission(permission)
        return singlePermission?.granted ?: false
    }

    fun initService(context: Context) {
        AllPermissionGroups.forEach { it.init(context) }
        allInit = true
        if (this::mainActivity.isInitialized) {
            initFlow.value = true
        }
        updateAll()
        Log.v(AppConstants.TAG, "Permissions allGranted=" + allGranted.toString())
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
    ): Boolean {
        val permission = singlePermission.name
        when (permission) {
            NOTIFICATION_LISTENER -> {
                val settingsIntent =
                    Intent(ACTION_LISTENER_SETTING)
                mainActivity.startActivity(settingsIntent)
                return false
            }

            else -> {
                if (mainActivity.shouldShowRequestPermissionRationale(permission))
                    return true
            }
        }
        permissionsLauncher.launch(arrayOf(permission))
        return false
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

    fun updateAll() {
        AllPermissionGroups.forEach {
            it.update()
        }
        collectMissing()
    }
}
