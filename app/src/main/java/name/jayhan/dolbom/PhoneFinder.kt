package name.jayhan.dolbom

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.getBroadcast
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow

class PhoneFinder(
    private val context: Context
): BroadcastReceiver() {
    private val notiMan = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager
    private val alarmMan = context.getSystemService(Context.ALARM_SERVICE)
            as AlarmManager
    private val alarmListener = AlarmListener()
    private val audioMan = context.getSystemService(Context.AUDIO_SERVICE)
            as AudioManager
    private var currentVol = 0
    private val mediaPlayer = MediaPlayer.create(context, Settings.System.DEFAULT_RINGTONE_URI)
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE)
            as CameraManager
    private val cameraId = cameraManager.cameraIdList[0]
    
    val available = MutableStateFlow(false)
    
    init {
        try {
            val channel = NotificationChannel(
                Const.CHANNEL_FIND,
                context.getString(R.string.find_phone),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                setShowBadge(false)
                enableVibration(true)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                description = context.getString(R.string.find_channel)
            }
            notiMan.createNotificationChannel(channel)
            available.value = true
        } catch (_: Exception) {
        }
        
        val filter = IntentFilter().apply {
            addAction(Const.INTENT_FIND)
            addAction(Const.INTENT_REPEAT)
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
        postNotification()
        
        currentVol = audioMan.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVol = audioMan.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioMan.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)
        mediaPlayer.start()
        
        cameraManager.setTorchMode(cameraId, true)
    }
    
    fun stop() {
        Log.v(Const.TAG, "Found!")
        cameraManager.setTorchMode(cameraId, false)
        mediaPlayer.stop()
        audioMan.setStreamVolume(AudioManager.STREAM_MUSIC, currentVol, 0)
        notiMan.cancel(Const.NOTI_FIND)
        alarmMan.cancel(alarmListener)
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Const.INTENT_FIND -> start()
            Const.INTENT_FOUND -> stop()
            Const.INTENT_REPEAT -> postNotification()
        }
    }
    
    fun postNotification() {
        val foundIntent = getBroadcast(
            context,
            Const.FOUND_REQUEST,
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
            setCategory(Notification.CATEGORY_ALARM)
            setSmallIcon(R.mipmap.ic_noti)
            setVisibility(Notification.VISIBILITY_PUBLIC)
        }.build()
        notiMan.notify(Const.NOTI_FIND, notification)
        
        alarmMan.set(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 15_000,
            null,
            alarmListener,
            null
        )
    }
    
    inner class AlarmListener:
        AlarmManager.OnAlarmListener
    {
        override fun onAlarm() {
            postNotification()
        }
    }
}
