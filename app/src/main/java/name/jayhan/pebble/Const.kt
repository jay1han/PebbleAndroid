package name.jayhan.pebble

import android.app.Notification
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID
import kotlin.time.Instant

object AppConst {
    const val TAG = "Dolbom"
    const val GITHUB_ANDROID = "https://github.com/jay1han/PebbleAndroid"
    const val GITHUB_PEBBLE = "https://github.com/jay1han/Pebble"
    val buildDateTime = Instant
        .fromEpochMilliseconds(BuildConfig.BUILDTIME)
        .formatDateTime()

    val titleSize = 28.sp
    val textSize = 20.sp
    val smallSize = 16.sp
    val subSize = 12.sp

    const val INTENT_REVIVE = "name.jayhan.pebble.REVIVE"

    const val INTENT_SEND_PEBBLE = "name.jayhan.pebble.SEND_PEBBLE"
    const val EXTRA_MSG_TYPE = "msg_type"
    const val EXTRA_PHONE_CHG = "phone_chg"
    const val EXTRA_PHONE_BATT = "phone_batt"
    const val EXTRA_TZ_MIN = "tz_min"
    const val EXTRA_WIFI = "wifi"
    const val EXTRA_NET = "net"
    const val EXTRA_SIM = "sim"
    const val EXTRA_CARRIER = "carrier"
    const val EXTRA_NOTI = "noti"
    const val EXTRA_BTID = "btid"
    const val EXTRA_BTC = "btc"

    const val CHANNEL_ID = "Dolbom"

    const val PREF_INDIC = "name.jayhan.pebble.INDICATORS"
    const val PREF_HISTORY = "name.jayhan.pebble.HISTORY"
    const val HIST_DISCHG_RATE = "dischg_rate"
    const val HIST_N_CYCLES = "n_cycles"
    const val HIST_UNPLUG_TIME = "unplug_time"
    const val HIST_PLUG_STATE = "plug_state"
    const val HIST_UNPLUG_LEVEL = "unplug_level"
    const val HIST_INIT_DATE = "init_date"

    val APP_UUID: UUID? = UUID.fromString("aaaab139-d4d0-478f-81f4-4cbbe4992461")
    const val MAX_NOTI_INDICATORS = 15
    const val MAX_LEN_ID = 19
}

enum class FilterType {
    Title { override val r = R.string.filter_title },
    Text { override val r = R.string.filter_text },
    Subject { override val r = R.string.filter_subject };
    
    abstract val r: Int
}

val FilterTypeStringIdList = FilterType.entries.map { it.r }

val FilterTypeExtra = listOf(
    Notification.EXTRA_TITLE,
    Notification.EXTRA_TEXT,
    Notification.EXTRA_CONVERSATION_TITLE,
)

fun getFilterType(
    index: Int
): FilterType {
    val values = listOf(FilterType.Title, FilterType.Text, FilterType.Subject)
    return (
            if (index in 0..< FilterType.entries.size) values[index]
            else FilterType.Title
            )
}
