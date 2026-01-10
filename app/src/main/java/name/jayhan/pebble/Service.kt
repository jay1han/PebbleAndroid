package name.jayhan.pebble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.getBroadcast
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
import android.net.ConnectivityManager
import android.os.IBinder
import android.telephony.TelephonyManager

class PebbleService(): Service() {
    private lateinit var context: Context
    private lateinit var connMan: ConnectivityManager
    private lateinit var teleMan: TelephonyManager
    private val delReceiver = DelReceiver()
    private val stopReceiver = StopReceiver()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        context = applicationContext

        val filter1 = IntentFilter().apply { addAction(AppConstants.INTENT_SERVICE_STOP) }
        context.registerReceiver(stopReceiver, filter1, RECEIVER_EXPORTED)
        val filter2 = IntentFilter().apply { addAction(AppConstants.INTENT_REVIVE) }
        context.registerReceiver(delReceiver, filter2,RECEIVER_EXPORTED)
        connMan = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        teleMan = context.getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        try {
            val notiMan = context.getSystemService(NOTIFICATION_SERVICE)
                    as NotificationManager
            val channel = NotificationChannel(
                AppConstants.CHANNEL_ID,
                getString(R.string.app_title),
                IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
                description = getString(R.string.channel_description)
            }
            notiMan.createNotificationChannel(channel)

            setupForeground()
        } catch (e: Exception) {
            println(e)
        }

        reInit()

        Pebble.askInfo()

        return super.onStartCommand(intent, flags, startId)
    }

    private fun reInit() {
        Pebble.init(context)
        Notifications.init(context)

        BatteryReceiver.init(context)
        BluetoothReceiver.init(context)

        WifiCallback.init(connMan)
        PhoneCallback.init(teleMan, context)
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
        }.build()
        this.startForeground(
            1,
            notification,
            FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        )
    }

    inner class DelReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            setupForeground()
            reInit()
        }
    }

    inner class StopReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == AppConstants.INTENT_SERVICE_STOP) {
                context.unregisterReceiver(delReceiver)
                context.unregisterReceiver(stopReceiver)
                stopSelf()
            }
        }
    }
}
