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
        Log.v(AppConst.TAG, "Service starting")
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
            Intent(AppConst.INTENT_REVIVE),
            PendingIntent.FLAG_IMMUTABLE
        )

        val filter = IntentFilter().apply {
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

            setupForeground()
        } catch (e: Exception) {
            println(e)
        }

        return super.onStartCommand(intent, flags, startId)
    }

    fun restartForeground() {
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

    private fun setupForeground() {
        Log.v(AppConst.TAG, "Running foreground")

        restartForeground()

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
                AppConst.INTENT_REVIVE -> {
                    Log.v(AppConst.TAG, "Restart service")
                    setupForeground()
                }

                AppConst.INTENT_SEND_PEBBLE -> {
                    val msgType = intent.getIntExtra(AppConst.EXTRA_MSG_TYPE, 0)

                    val pebbleDict = PebbleDictionary()
                    pebbleDict.addInt8(DictKey.MSG_TYPE.code, msgType.toByte())

                    when(msgType) {
                        MsgType.INFO.code,
                        MsgType.WBATT.code -> {
                        }

                        MsgType.TZ.code -> {
                            val minutes = intent.getIntExtra(AppConst.EXTRA_TZ_MIN, 0)
                            pebbleDict.addInt16(DictKey.TZ_MIN.code, minutes.toShort())
                        }

                        MsgType.PHONE_CHG.code -> {
                            val isCharging = intent.getIntExtra(AppConst.EXTRA_PHONE_CHG, 0)
                            pebbleDict.addInt8(DictKey.PHONE_CHG.code, isCharging.toByte())
                            val percent = intent.getIntExtra(AppConst.EXTRA_PHONE_BATT, 0)
                            pebbleDict.addInt8(DictKey.PHONE_BATT.code, percent.toByte())
                        }

                        MsgType.WIFI.code -> {
                            val ssid = intent.getStringExtra(AppConst.EXTRA_WIFI) ?: ""
                            pebbleDict.addString(DictKey.WIFI.code, ssid.take(AppConst.MAX_LEN_ID))
                        }

                        MsgType.NET.code -> {
                            val gen = intent.getIntExtra(AppConst.EXTRA_NET, 0)
                            pebbleDict.addInt8(DictKey.NET.code, gen.toByte())
                            val sim = intent.getIntExtra(AppConst.EXTRA_SIM, 0)
                            pebbleDict.addInt8(DictKey.SIM.code, sim.toByte())
                            val carrier = intent.getStringExtra(AppConst.EXTRA_CARRIER) ?: ""
                            pebbleDict.addString(DictKey.CARRIER.code, carrier.take(AppConst.MAX_LEN_ID))
                        }

                        MsgType.NOTI.code -> {
                            val noti = intent.getStringExtra(AppConst.EXTRA_NOTI) ?: ""
                            pebbleDict.addString(DictKey.NOTI.code, noti.take(AppConst.MAX_NOTI_INDICATORS))
                        }

                        MsgType.BT.code -> {
                            val btid = intent.getStringExtra(AppConst.EXTRA_BTID) ?: ""
                            pebbleDict.addString(DictKey.BTID.code, btid.take(AppConst.MAX_LEN_ID))
                            val btc = intent.getIntExtra(AppConst.EXTRA_BTC, 0)
                            pebbleDict.addInt8(DictKey.BTC.code, btc.toByte())

                        }
                    }

                    Pebble.sendData(context, pebbleDict)
                }
            }
            if (Pebble.doRefresh) {
                Pebble.doRefresh = false
                Pebble.sendIntent(context, MsgType.WBATT) {}
                Notifications.refresh(context)

                batteryReceiver.refresh()
                bluetoothReceiver.refresh()

                wifiCallback.refresh()
                phoneCallback.refresh()
            }

            restartForeground()
        }
    }
}
