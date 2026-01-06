package name.jayhan.pebble

import android.app.NotificationChannel
import android.app.NotificationChannel.DEFAULT_CHANNEL_ID
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class PebbleService(): Service() {
    private val binder = LocalBinder()

    inner class LocalBinder: Binder() {
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val notiMan = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel("PebbleService", "Pebble", IMPORTANCE_LOW)
            channel.description = "Notification channel"
            notiMan.createNotificationChannel(channel)
            val notification = NotificationCompat.Builder(applicationContext, "PebbleService")
                .setContentTitle("Pebble")
                .setContentText("Keep the Pebble service active")
                .build()
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
}