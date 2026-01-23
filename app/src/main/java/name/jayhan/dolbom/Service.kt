package name.jayhan.dolbom

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
import android.os.IBinder
import android.util.Log

class PebbleService:
    Service()
{
    private lateinit var context: Context
    private lateinit var notiMan: NotificationManager
    private lateinit var batteryReceiver: BatteryReceiver
    private lateinit var bluetoothReceiver: BluetoothReceiver
    private lateinit var wifiCallback: WifiCallback
    private lateinit var phoneCallback: PhoneCallback

    private val receiver = Receiver()
    private lateinit var reviveIntent: PendingIntent
    private lateinit var launchIntent: PendingIntent
    private lateinit var phoneFinder: PhoneFinder
    private val protocol = Protocol()

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.v(Const.TAG, "Service starting Id=$startId")
        if (!Permissions.allGranted) {
            Log.v(Const.TAG, "Permissions missing")
            stopSelf()
            return START_NOT_STICKY
        }

        context = applicationContext

        launchIntent = PendingIntent.getActivity(
            context, Const.LAUNCH_REQUEST,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE
        )
        reviveIntent = getBroadcast(
            context, Const.REVIVE_REQUEST,
            Intent(Const.INTENT_RESTART),
            PendingIntent.FLAG_IMMUTABLE
        )

        val filter = IntentFilter().apply {
            addAction(Const.INTENT_RESTART)
            addAction(Const.INTENT_STOP)
            addAction(Const.INTENT_UPDATE)
            addAction(Const.INTENT_REFRESH)
            addAction(Const.INTENT_SEND_PEBBLE)
        }
        context.registerReceiver(receiver, filter,RECEIVER_EXPORTED)

        val channel = NotificationChannel(
            Const.CHANNEL_ID,
            getString(R.string.app_title),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_SECRET
            description = getString(R.string.channel_description)
            importance = NotificationManager.IMPORTANCE_LOW
        }
        
        notiMan = context.getSystemService(NOTIFICATION_SERVICE)
                as NotificationManager
        notiMan.createNotificationChannel(channel)
        
        startForeground(Const.NOTI_SERVICE, buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        )

        try {
            restartService()
        } catch (e: Exception) {
            Log.e(Const.TAG, e.toString())
            stopSelf()
        }

        return super.onStartCommand(intent, flags, startId)
    }
    
    override fun onDestroy() {
        Log.v(Const.TAG, "Destroy Service")
        stopService()
        stopModules()
        super.onDestroy()
    }
    
    fun updateNofitication() {
        Log.v(Const.TAG, "Update Notification")
        notiMan.notify(Const.NOTI_SERVICE, buildNotification())
    }
    
    fun buildNotification(): Notification {
        return Notification.Builder(
            context,
            Const.CHANNEL_ID
        ).apply {
            setFlag(Notification.FLAG_FOREGROUND_SERVICE, true)
            setOngoing(true)
            setDeleteIntent(reviveIntent)
            setContentIntent(launchIntent)
            setContentTitle("${Pebble.watchInfo.modelString()} ${Pebble.watchInfo.battery}%")
            setContentText("")
            setSmallIcon(R.mipmap.ic_launcher)
            setVisibility(Notification.VISIBILITY_SECRET)
        }.build()
    }
    
    fun restartService() {
        Log.v(Const.TAG, "RestartService")
        
        Notifications.init(context)
        History.init(context)
        Pebble.init(context)
        
        stopModules()
        phoneFinder = PhoneFinder(context)
        batteryReceiver = BatteryReceiver(context)
        bluetoothReceiver = BluetoothReceiver(context)
        wifiCallback = WifiCallback(context)
        phoneCallback = PhoneCallback(context)
        Log.v(Const.TAG, "Modules started")
    }
    
    fun refreshService() {
        Log.v(Const.TAG, "RefreshService")
        protocol.reset()
        batteryReceiver.refresh()
        bluetoothReceiver.refresh()
        wifiCallback.refresh()
        phoneCallback.refresh()
        Notifications.refresh(context)
    }
    
    private fun stopService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        notiMan.deleteNotificationChannel(Const.CHANNEL_ID)
        context.unregisterReceiver(receiver)
    }
    
    private fun stopModules() {
        if (this::batteryReceiver.isInitialized) batteryReceiver.deinit()
        if (this::bluetoothReceiver.isInitialized) bluetoothReceiver.deinit()
        if (this::wifiCallback.isInitialized) wifiCallback.deinit()
        if (this::phoneCallback.isInitialized) phoneCallback.deinit()
        if (this::phoneFinder.isInitialized) phoneFinder.deinit()
        Log.v(Const.TAG, "Modules stopped")
    }

    inner class Receiver: BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (!Permissions.allGranted) {
                Log.v(Const.TAG, "Permissions revoked, stop")
                stopService()
                stopSelf()
            }
            
            when(intent.action) {
                Const.INTENT_RESTART -> {
                    Log.v(Const.TAG, "Intent: Restart service")
                    restartService()
                }

                Const.INTENT_UPDATE -> {
                    Log.v(Const.TAG, "Intent: Update notification")
                    updateNofitication()
                }

                Const.INTENT_REFRESH -> {
                    Log.v(Const.TAG, "Intent: Refresh service")
                    refreshService()
                }
                
                Const.INTENT_STOP -> {
                    Log.v(Const.TAG, "Intent: Stop service")
                    stopSelf()
                }
                
                Const.INTENT_SEND_PEBBLE ->
                    protocol.send(context, intent)
            }
        }
    }
}
