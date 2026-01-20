package name.jayhan.pebble

import androidx.compose.ui.unit.sp
import java.util.UUID
import kotlin.time.Instant

object Const {
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

    const val INTENT_RESTART = "name.jayhan.pebble.RESTART_SERVICE"
    const val INTENT_REVIVE = "name.jayhan.pebble.REVIVE_SERVICE"
    const val INTENT_UPDATE = "name.jayhan.pebble.UPDATE_SERVICE"
    const val INTENT_FIND = "name.jayhan.pebble.FIND_PHONE"
    const val INTENT_FOUND = "name.jayhan.pebble.FOUND"

    const val INTENT_SEND_PEBBLE = "name.jayhan.pebble.SEND_PEBBLE"
    const val EXTRA_MSG_TYPE = "msg_type"
    const val EXTRA_PHONE_CHG = "phone_chg"
    const val EXTRA_PHONE_PLUG = "phone_plug"
    const val EXTRA_PHONE_BATT = "phone_batt"
    const val EXTRA_TZ_MIN = "tz_min"
    const val EXTRA_WIFI = "wifi"
    const val EXTRA_NET = "net"
    const val EXTRA_SIM = "sim"
    const val EXTRA_CARRIER = "carrier"
    const val EXTRA_NOTI = "noti"
    const val EXTRA_BTID = "btid"
    const val EXTRA_BTC = "btc"
    const val EXTRA_BTON = "bton"

    const val CHANNEL_ID = "Dolbom"
    const val NOTI_SERVICE = 10
    const val LAUNCH_REQUEST = 11
    const val REVIVE_REQUEST = 12
    const val CHANNEL_FIND = "Dolbom Find"
    const val NOTI_FIND = 20
    const val FOUND_REQUEST = 21
    const val FINDING_REQUEST = 22
    const val INTENT_FINDING = "name.jayhan.pebble.FINDING_BIG"
    
    const val PREF_INDIC = "name.jayhan.pebble.INDICATORS"
    const val PREF_HISTORY = "name.jayhan.pebble.HISTORY"
    const val HIST_DISCHG_RATE = "dischg_rate"
    const val HIST_N_CYCLES = "n_cycles"
    const val HIST_UNPLUG_TIME = "unplug_time"
    const val HIST_PLUG_STATE = "plug_state"
    const val HIST_UNPLUG_LEVEL = "unplug_level"
    const val HIST_INIT_DATE = "init_date"

    const val MAX_NOTI_INDICATORS = 15
    const val MAX_LEN_ID = 19
}
