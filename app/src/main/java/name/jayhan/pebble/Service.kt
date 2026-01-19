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
import android.os.IBinder
import android.util.Log
import com.getpebble.android.kit.util.PebbleDictionary

class PebbleService: Service()
{
    private lateinit var context: Context
    private lateinit var batteryReceiver: BatteryReceiver
    private lateinit var bluetoothReceiver: BluetoothReceiver
    private lateinit var wifiCallback: WifiCallback
    private lateinit var phoneCallback: PhoneCallback

    private val receiver = Receiver()
    private lateinit var reviveIntent: PendingIntent
    private lateinit var launchIntent: PendingIntent

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.v(Const.TAG, "Service starting Id=$startId")
        context = applicationContext
        val activityIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        launchIntent = PendingIntent.getActivity(
            context, 2,
            activityIntent,
            PendingIntent.FLAG_IMMUTABLE
            )
        reviveIntent = getBroadcast(
            context,
            1,
            Intent(Const.INTENT_RESTART),
            PendingIntent.FLAG_IMMUTABLE
        )

        val filter = IntentFilter().apply {
            addAction(Const.INTENT_RESTART)
            addAction(Const.INTENT_UPDATE)
            addAction(Const.INTENT_REVIVE)
            addAction(Const.INTENT_SEND_PEBBLE)
        }
        context.registerReceiver(receiver, filter,RECEIVER_EXPORTED)

        Permissions.initService(context)

        try {
            val notiMan = context.getSystemService(NOTIFICATION_SERVICE)
                    as NotificationManager
            val channel = NotificationChannel(
                Const.CHANNEL_ID,
                getString(R.string.app_title),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
                description = getString(R.string.channel_description)
            }
            notiMan.createNotificationChannel(channel)

            restartService()
            updateService()
        } catch (e: Exception) {
            println(e)
        }

        return super.onStartCommand(intent, flags, startId)
    }

    fun updateService() {
        Log.v(Const.TAG, "UpdateService")
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

        this.startForeground(
            1,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        )
    }
    
    fun restartService() {
        Log.v(Const.TAG, "RestartService")
        if (Permissions.allGranted) {
            Pebble.init(context)
            Notifications.init(context)
            History.init(context)

            batteryReceiver = BatteryReceiver(context)
            bluetoothReceiver = BluetoothReceiver(context)

            wifiCallback = WifiCallback(context)
            phoneCallback = PhoneCallback(context)
        } else {
            Log.v(Const.TAG, "Permissions missing")
        }
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
                    updateService()
                }

                Const.INTENT_REVIVE -> {
                    Log.v(Const.TAG, "Intent: Revive service")
                    restartService()
                }

                Const.INTENT_SEND_PEBBLE -> {
                    val msgType = intent.getIntExtra(Const.EXTRA_MSG_TYPE, 0)

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

                        }
                    }

                    Pebble.sendData(context, pebbleDict)
                }
            }
        }
    }
}
