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
import android.os.IBinder
import android.util.Log
import com.getpebble.android.kit.util.PebbleDictionary

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

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
    
    private var startId = 0
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.v(Const.TAG, "Service starting Id=$startId")
        if (this.startId == 0) this.startId = startId
        else {
            Log.v(Const.TAG, "Skip")
            return super.onStartCommand(intent, flags, startId)
        }

        context = applicationContext
        notiMan = context.getSystemService(NOTIFICATION_SERVICE)
                as NotificationManager
        
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
            addAction(Const.INTENT_UPDATE)
            addAction(Const.INTENT_REFRESH)
            addAction(Const.INTENT_SEND_PEBBLE)
        }
        context.registerReceiver(receiver, filter,RECEIVER_EXPORTED)

        Permissions.initService(context)

        try {
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
            notiMan.createNotificationChannel(channel)

            restartService()
            updateNofitication()
        } catch (_: Exception) {
        }

        return super.onStartCommand(intent, flags, startId)
    }
    
    override fun onDestroy() {
        Log.v(Const.TAG, "Destroy Service")
        context.unregisterReceiver(receiver)
        stopModules()
        super.onDestroy()
    }
    
    fun updateNofitication() {
        Log.v(Const.TAG, "Update Notification")
        val notification = Notification.Builder(
            context,
            Const.CHANNEL_ID
        ).apply {
            setDeleteIntent(reviveIntent)
            setContentIntent(launchIntent)
            setContentTitle("${Pebble.watchInfo.modelString()} ${Pebble.watchInfo.battery}%")
            setContentText("")
            setSmallIcon(R.mipmap.ic_launcher)
            setVisibility(Notification.VISIBILITY_SECRET)
        }.build()
        
        notiMan.notify(Const.NOTI_SERVICE, notification)
    }
    
    fun restartService() {
        Log.v(Const.TAG, "RestartService")
        if (Permissions.allGranted) {
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

        } else {
            Log.v(Const.TAG, "Permissions missing")
        }
    }
    
    fun refreshService() {
        Log.v(Const.TAG, "RefreshService")
        batteryReceiver.refresh()
        bluetoothReceiver.refresh()
        wifiCallback.refresh()
        phoneCallback.refresh()
        Notifications.refresh(context)
    }
    
    fun stopModules() {
        if (this::batteryReceiver.isInitialized) batteryReceiver.deinit()
        if (this::bluetoothReceiver.isInitialized) bluetoothReceiver.deinit()
        if (this::wifiCallback.isInitialized) wifiCallback.deinit()
        if (this::phoneCallback.isInitialized) phoneCallback.deinit()
        if (this::phoneFinder.isInitialized) phoneFinder.deinit()
        Log.v(Const.TAG, "Modules stopped")
    }

    inner class Receiver: BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                Const.INTENT_RESTART -> {
                    Log.v(Const.TAG, "Intent: Restart service")
                    restartService()
                }

                Const.INTENT_UPDATE -> {
                    Log.v(Const.TAG, "Intent: Update service")
                    updateNofitication()
                }

                Const.INTENT_REFRESH -> {
                    Log.v(Const.TAG, "Intent: Refresh service")
                    refreshService()
                }
                
                Const.INTENT_SEND_PEBBLE -> {
                    val msgType = intent.getIntExtra(Const.EXTRA_MSG_TYPE, 0)
                    Log.v(Const.TAG, "out $msgType")

                    val pebbleDict = PebbleDictionary()
                    pebbleDict.addInt8(DictKey.MSG_TYPE.ordinal, msgType.toByte())

                    when(msgType) {
                        MsgType.INFO.ordinal,
                        MsgType.WBATT.ordinal -> {
                        }

                        MsgType.TZ.ordinal -> {
                            val minutes = intent.getIntExtra(Const.EXTRA_TZ_MIN, 0)
                            pebbleDict.addInt16(DictKey.TZ_MIN.ordinal, minutes.toShort())
                        }

                        MsgType.PHONE_CHG.ordinal -> {
                            val isCharging = intent.getIntExtra(Const.EXTRA_PHONE_CHG, 0)
                            pebbleDict.addInt8(DictKey.PHONE_CHG.ordinal, isCharging.toByte())
                            val isPlugged = intent.getIntExtra(Const.EXTRA_PHONE_PLUG, 0)
                            pebbleDict.addInt8(DictKey.PHONE_PLUG.ordinal, isPlugged.toByte())
                            val percent = intent.getIntExtra(Const.EXTRA_PHONE_BATT, 0)
                            pebbleDict.addInt8(DictKey.PHONE_BATT.ordinal, percent.toByte())
                        }

                        MsgType.WIFI.ordinal -> {
                            val ssid = intent.getStringExtra(Const.EXTRA_WIFI) ?: ""
                            pebbleDict.addString(DictKey.WIFI.ordinal, ssid.take(Const.MAX_LEN_ID))
                        }

                        MsgType.NET.ordinal -> {
                            val gen = intent.getIntExtra(Const.EXTRA_NET, 0)
                            pebbleDict.addInt8(DictKey.NET.ordinal, gen.toByte())
                            val sim = intent.getIntExtra(Const.EXTRA_SIM, 0)
                            pebbleDict.addInt8(DictKey.SIM.ordinal, sim.toByte())
                            val carrier = intent.getStringExtra(Const.EXTRA_CARRIER) ?: ""
                            pebbleDict.addString(DictKey.CARRIER.ordinal, carrier.take(Const.MAX_LEN_ID))
                        }

                        MsgType.NOTI.ordinal -> {
                            val noti = intent.getStringExtra(Const.EXTRA_NOTI) ?: ""
                            pebbleDict.addString(DictKey.NOTI.ordinal, noti.take(Const.MAX_NOTI_INDICATORS))
                        }

                        MsgType.BT.ordinal -> {
                            val btid = intent.getStringExtra(Const.EXTRA_BTID) ?: ""
                            pebbleDict.addString(DictKey.BTID.ordinal, btid.take(Const.MAX_LEN_ID))
                            val btc = intent.getIntExtra(Const.EXTRA_BTC, 0)
                            pebbleDict.addInt8(DictKey.BTC.ordinal, btc.toByte())
                            val bton = intent.getBooleanExtra(Const.EXTRA_BTON, false)
                            pebbleDict.addInt8(DictKey.BTON.ordinal, if (bton) 1.toByte() else 0.toByte())
                        }
                    }

                    Pebble.sendData(context, pebbleDict)
                }
            }
        }
    }
}
