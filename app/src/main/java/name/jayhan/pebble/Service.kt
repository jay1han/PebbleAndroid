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
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

class PebbleService(): Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val filter = IntentFilter()
            .apply {
                addAction("name.jayhan.pebble.REVIVE_BACKGROUND")
            }
        val delReceiver = DelReceiver()
        ContextCompat.registerReceiver(applicationContext, delReceiver, filter,RECEIVER_EXPORTED)

        try {
            val notiMan = applicationContext.getSystemService(NOTIFICATION_SERVICE)
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

        return super.onStartCommand(intent, flags, startId)
    }

    fun buildNotification(count: Int): Notification {
        val delIntent = Intent("name.jayhan.pebble.REVIVE_FOREGROUND")
            .apply {
                putExtra("count", count + 1)
            }
        val pendingIntent = getBroadcast(
            applicationContext,
            1,
            delIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(
            applicationContext,
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
