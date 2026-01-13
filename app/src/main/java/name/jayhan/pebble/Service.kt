package name.jayhan.pebble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.getBroadcast
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.os.IBinder
import android.telephony.TelephonyManager

class PebbleService():
    Service() {

    private lateinit var context: Context
    private lateinit var connMan: ConnectivityManager
    private lateinit var teleMan: TelephonyManager
    private lateinit var batteryReceiver: BatteryReceiver
    private lateinit var bluetoothReceiver: BluetoothReceiver
    private lateinit var wifiCallback: WifiCallback
    private lateinit var phoneCallback: PhoneCallback

    private val delReceiver = DelReceiver()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        context = applicationContext

        val filter = IntentFilter().apply { addAction(AppConstants.INTENT_REVIVE) }
        context.registerReceiver(delReceiver, filter,RECEIVER_EXPORTED)
        connMan = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        teleMan = context.getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        try {
            val notiMan = context.getSystemService(NOTIFICATION_SERVICE)
                    as NotificationManager
            val channel = NotificationChannel(
                AppConstants.CHANNEL_ID,
                getString(R.string.app_title),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
                description = getString(R.string.channel_description)
            }
            notiMan.createNotificationChannel(channel)

            setupForeground()
        } catch (e: Exception) {
            println(e)
        }

        reInit()

        Pebble.askInfo()
        Notifications.refresh()

        return super.onStartCommand(intent, flags, startId)
    }

    private fun reInit() {
        Pebble.init(context)
        Notifications.init(context)

        batteryReceiver = BatteryReceiver(context)
        bluetoothReceiver = BluetoothReceiver(context)

        wifiCallback = WifiCallback(connMan)
        phoneCallback = PhoneCallback(teleMan, context)
    }

    private fun setupForeground() {
        val delIntent = Intent(AppConstants.INTENT_REVIVE)
        val pendingIntent = getBroadcast(
            context,
            1,
            delIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(
            context,
            AppConstants.CHANNEL_ID
        ).apply {
            setDeleteIntent(pendingIntent)
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentTitle("")
            setContentText("")
            setVisibility(Notification.VISIBILITY_SECRET)
        }.build()
        this.startForeground(
            1,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        )
    }

    inner class DelReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            setupForeground()
            reInit()
        }
    }
}
