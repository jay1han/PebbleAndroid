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
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.MutableStateFlow
import name.jayhan.pebble.ui.theme.PebbleTheme

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
            setSmallIcon(R.mipmap.ic_noti)
            setVisibility(Notification.VISIBILITY_PUBLIC)
        }.build()
        notiMan.notify(Const.NOTI_FIND, notification)
        
//        context.startActivity(Intent(context, FinderActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        context.startActivity(Intent(Const.INTENT_FINDING).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
//        mediaPlayer = MediaPlayer.create(context, 0)
    }
    
    fun stop() {
        Log.v(Const.TAG, "Found!")
        notiMan.cancel(Const.NOTI_FIND)
//        mediaPlayer.release()
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Const.INTENT_FIND -> start()
            Const.INTENT_FOUND -> stop()
        }
    }
}

class FinderActivity:
    ComponentActivity()
{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.v(Const.TAG, "Start activity")
        val context = applicationContext
        
        enableEdgeToEdge()
        setContent {
            PebbleTheme {
                FindingScreen {
                    context.sendBroadcast(Intent(Const.INTENT_FOUND))
                    finish()
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Const.INTENT_FOUND) {
            finish()
        }
    }
}

@Composable
fun FindingScreen(
    onClick: () -> Unit
) {
    Scaffold() { innerPadding ->
        Card (
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            Text(
                text = "Hello world"
            )
            Button(
                onClick = onClick
            ) {
                Text(
                    text = "OK"
                )
            }
        }
    }
}

@Preview
@Composable
fun FindingScreenPreview() {
    FindingScreen( {} )
}