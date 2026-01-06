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
import androidx.core.app.ServiceCompat

class PebbleService(): Service() {
    lateinit var context: Context

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        context = applicationContext

        val filter = IntentFilter()
            .apply {
                addAction("name.jayhan.pebble.REVIVE_BACKGROUND")
            }
        val delReceiver = DelReceiver()
        context.registerReceiver(delReceiver, filter,RECEIVER_EXPORTED)

        try {
            val notiMan = context.getSystemService(NOTIFICATION_SERVICE)
                    as NotificationManager
            val channel = NotificationChannel(
                "PebbleService",
                "Pebble",
                IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
                description = "Notification channel"
            }
            notiMan.createNotificationChannel(channel)

            val notification = buildNotification(0)

            ServiceCompat.startForeground(
                this,
                1,
                notification,
                FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } catch (e: Exception) {
            println(e)
        }

        val connMan = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        WiFiCallback(context, connMan)

        val teleMan = context.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        PhoneCallback(context, teleMan)

        return super.onStartCommand(intent, flags, startId)
    }

    fun buildNotification(count: Int): Notification {
        val delIntent = Intent("name.jayhan.pebble.REVIVE_FOREGROUND")
            .apply {
                putExtra("count", count + 1)
            }
        val pendingIntent = getBroadcast(
            context,
            1,
            delIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(
            context,
            "PebbleService"
        ).apply {
//            setDeleteIntent(pendingIntent)
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentTitle("Pebble Service")
            setContentText("Keep the Pebble service active")
        }.build()
        return notification
    }

    inner class DelReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val count = intent.getIntExtra("count", 1)
            val notification = buildNotification(count)
            val notiMan = context.getSystemService(NOTIFICATION_SERVICE)
                    as NotificationManager
            notiMan.notify(1, notification)
        }
    }
}
