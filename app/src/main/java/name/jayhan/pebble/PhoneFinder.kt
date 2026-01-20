package name.jayhan.pebble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.getBroadcast
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow

class PhoneFinder(
    private val context: Context
): BroadcastReceiver() {
    private val notiMan = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private lateinit var mediaPlayer: MediaPlayer
    val available = MutableStateFlow(false)
    
    init {
        try {
            val channel = NotificationChannel(
                Const.CHANNEL_FIND,
                context.getString(R.string.find_phone),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                description = context.getString(R.string.find_channel)
            }
            notiMan.createNotificationChannel(channel)
            available.value = true
        } catch (_: Exception) {
        }
        
        val filter = IntentFilter().apply {
            addAction(Const.INTENT_FIND)
            addAction(Const.INTENT_FOUND)
        }
        context.registerReceiver(this, filter,Context.RECEIVER_EXPORTED)
    }
    
    fun deinit() {
        context.unregisterReceiver(this)
        available.value = false
    }
    
    fun start() {
        if (!available.value) {
            Log.v(Const.TAG, "Find phone unavailable")
            return
        }
        
        Log.v(Const.TAG, "Find phone")
        val foundIntent = getBroadcast(
            context,
            3,  //
            Intent(Const.INTENT_FOUND),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(
            context,
            Const.CHANNEL_FIND
        ).apply {
            setDeleteIntent(foundIntent)
            setContentIntent(foundIntent)
            setContentTitle("Find phone")
            setContentText("Click to stop")
            setSmallIcon(R.mipmap.ic_noti)
            setVisibility(Notification.VISIBILITY_PUBLIC)
        }.build()
        notiMan.notify(
            3,  // TODO
            notification
        )
        
//        mediaPlayer = MediaPlayer.create(context, 0)
    }
    
    fun stop() {
        Log.v(Const.TAG, "Found!")
        notiMan.cancel(3)
//        mediaPlayer.release()
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Const.INTENT_FIND -> start()
            Const.INTENT_FOUND -> stop()
        }
    }
}