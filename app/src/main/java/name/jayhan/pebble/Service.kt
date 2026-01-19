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
        Log.v(AppConst.TAG, "Service starting Id=$startId")
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
            Intent(AppConst.INTENT_RESTART),
            PendingIntent.FLAG_IMMUTABLE
        )

        val filter = IntentFilter().apply {
            addAction(AppConst.INTENT_RESTART)
            addAction(AppConst.INTENT_UPDATE)
            addAction(AppConst.INTENT_REVIVE)
            addAction(AppConst.INTENT_SEND_PEBBLE)
        }
        context.registerReceiver(receiver, filter,RECEIVER_EXPORTED)

        Permissions.initService(context)

        try {
            val notiMan = context.getSystemService(NOTIFICATION_SERVICE)
                    as NotificationManager
            val channel = NotificationChannel(
                AppConst.CHANNEL_ID,
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
        Log.v(AppConst.TAG, "UpdateService")
        val notification = Notification.Builder(
            context,
            AppConst.CHANNEL_ID
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
        Log.v(AppConst.TAG, "RestartService")
        if (Permissions.allGranted) {
            Pebble.init(context)
            Notifications.init(context)
            History.init(context)

            batteryReceiver = BatteryReceiver(context)
            bluetoothReceiver = BluetoothReceiver(context)

            wifiCallback = WifiCallback(context)
            phoneCallback = PhoneCallback(context)
        } else {
            Log.v(AppConst.TAG, "Permissions missing")
        }
    }

    inner class Receiver: BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                AppConst.INTENT_RESTART -> {
                    Log.v(AppConst.TAG, "Intent: Restart service")
                    restartService()
                }

                AppConst.INTENT_UPDATE -> {
                    Log.v(AppConst.TAG, "Intent: Update service")
                    updateService()
                }

                AppConst.INTENT_REVIVE -> {
                    Log.v(AppConst.TAG, "Intent: Revive service")
                    restartService()
                }

                AppConst.INTENT_SEND_PEBBLE -> {
                    val msgType = intent.getIntExtra(AppConst.EXTRA_MSG_TYPE, 0)

                    val pebbleDict = PebbleDictionary()
                    pebbleDict.addInt8(DictKey.MSG_TYPE.ordinal, msgType.toByte())

                    when(msgType) {
                        MsgType.INFO.ordinal,
                        MsgType.WBATT.ordinal -> {
                        }

                        MsgType.TZ.ordinal -> {
                            val minutes = intent.getIntExtra(AppConst.EXTRA_TZ_MIN, 0)
                            pebbleDict.addInt16(DictKey.TZ_MIN.ordinal, minutes.toShort())
                        }

                        MsgType.PHONE_CHG.ordinal -> {
                            val isCharging = intent.getIntExtra(AppConst.EXTRA_PHONE_CHG, 0)
                            pebbleDict.addInt8(DictKey.PHONE_CHG.ordinal, isCharging.toByte())
                            val percent = intent.getIntExtra(AppConst.EXTRA_PHONE_BATT, 0)
                            pebbleDict.addInt8(DictKey.PHONE_BATT.ordinal, percent.toByte())
                        }

                        MsgType.WIFI.ordinal -> {
                            val ssid = intent.getStringExtra(AppConst.EXTRA_WIFI) ?: ""
                            pebbleDict.addString(DictKey.WIFI.ordinal, ssid.take(AppConst.MAX_LEN_ID))
                        }

                        MsgType.NET.ordinal -> {
                            val gen = intent.getIntExtra(AppConst.EXTRA_NET, 0)
                            pebbleDict.addInt8(DictKey.NET.ordinal, gen.toByte())
                            val sim = intent.getIntExtra(AppConst.EXTRA_SIM, 0)
                            pebbleDict.addInt8(DictKey.SIM.ordinal, sim.toByte())
                            val carrier = intent.getStringExtra(AppConst.EXTRA_CARRIER) ?: ""
                            pebbleDict.addString(DictKey.CARRIER.ordinal, carrier.take(AppConst.MAX_LEN_ID))
                        }

                        MsgType.NOTI.ordinal -> {
                            val noti = intent.getStringExtra(AppConst.EXTRA_NOTI) ?: ""
                            pebbleDict.addString(DictKey.NOTI.ordinal, noti.take(AppConst.MAX_NOTI_INDICATORS))
                        }

                        MsgType.BT.ordinal -> {
                            val btid = intent.getStringExtra(AppConst.EXTRA_BTID) ?: ""
                            pebbleDict.addString(DictKey.BTID.ordinal, btid.take(AppConst.MAX_LEN_ID))
                            val btc = intent.getIntExtra(AppConst.EXTRA_BTC, 0)
                            pebbleDict.addInt8(DictKey.BTC.ordinal, btc.toByte())

                        }
                    }

                    Pebble.sendData(context, pebbleDict)
                }
            }
        }
    }
}
