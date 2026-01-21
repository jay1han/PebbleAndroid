package name.jayhan.pebble

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.isDistantPast

class HistoryData(
    val initDate: Instant = Instant.DISTANT_PAST,
    val lastUnplug: Instant = Instant.DISTANT_PAST,
    val unpluggedLevel: Int = 0,
    val numberOfCycles: Int = 0,
    val dischargeRate: Float = 0f,
    val currentlyPlugged: Boolean = false,
) {
    fun isValid(): Boolean {
        return initDate != Instant.DISTANT_PAST
    }

    fun set(
        initDate: Instant? = null,
        lastUnplug: Instant? = null,
        unpluggedLevel: Int? = null,
        numberOfCycles: Int? = null,
        dischargeRate: Float? = null,
        currentlyPlugged: Boolean? = null,
    ): HistoryData {
        return HistoryData(
            initDate ?: this.initDate,
            lastUnplug ?:this.lastUnplug,
            unpluggedLevel ?: this.unpluggedLevel,
            numberOfCycles ?: this.numberOfCycles,
            dischargeRate ?: this.dischargeRate,
            currentlyPlugged ?: this.currentlyPlugged,
        )
    }
    
    companion object {
        fun read(prefs: SharedPreferences): HistoryData {
            return HistoryData(
                longToDate(prefs.getLong(Const.HIST_INIT_DATE, 0)),
                longToDate(prefs.getLong(Const.HIST_UNPLUG_TIME, 0L)),
                prefs.getInt(Const.HIST_UNPLUG_LEVEL, 0),
                prefs.getInt(Const.HIST_N_CYCLES, 0),
                prefs.getFloat(Const.HIST_DISCHG_RATE, 0f),
                prefs.getBoolean(Const.HIST_PLUG_STATE, false),
            )
        }
    }
}

private fun longToDate(dateLong: Long): Instant {
    return (
            if (dateLong == 0L) Instant.DISTANT_PAST
            else Instant.fromEpochSeconds(dateLong)
            )
}

object History {
    val historyFlow = MutableStateFlow(HistoryData())
    private lateinit var savedHistory: SharedPreferences

    fun init(context: Context) {
        savedHistory = context.getSharedPreferences(
            Const.PREF_HISTORY,
            Context.MODE_PRIVATE
        )

        val historyData = HistoryData.read(savedHistory)
        Log.v(
            Const.TAG,
            "History init ${historyData.numberOfCycles} since ${historyData.initDate.formatDate()} rate=${historyData.dischargeRate}, " +
                    "unplugged ${historyData.lastUnplug.formatDateTime()} at ${historyData.unpluggedLevel}%"
        )

        historyFlow.value = historyData
    }

    fun event(
        level: Int,
        plugged: Boolean
    ) {
        val hist = HistoryData.read(savedHistory)
        Log.v(Const.TAG, "History event ($level,$plugged)" +
                " when (${hist.unpluggedLevel},${hist.currentlyPlugged},${hist.lastUnplug})")

        when {
            plugged && !hist.currentlyPlugged ->
                if (!hist.lastUnplug.isDistantPast) {
                    val discharge = hist.unpluggedLevel - level
                    val duration = Clock.System.now() - hist.lastUnplug
                    Log.v(Const.TAG, "History cycle $discharge% in ${duration.inWholeSeconds}s")
                    if (discharge >= 10 && duration.inWholeSeconds > 3600) {
                        val inDays = duration.inWholeSeconds.toFloat() / (3600 * 24)
                        val dischargeRate = discharge.toFloat() / inDays
                        val newRate = (hist.dischargeRate * hist.numberOfCycles + dischargeRate) /
                                (hist.numberOfCycles + 1)
    
                        savedHistory.edit {
                            putInt(Const.HIST_N_CYCLES, hist.numberOfCycles + 1)
                            putFloat(Const.HIST_DISCHG_RATE, newRate)
                            putBoolean(Const.HIST_PLUG_STATE, plugged)
                        }
                        Log.v(Const.TAG, "History saved ${hist.numberOfCycles+1} cycles rate=$newRate")
    
                        historyFlow.value = hist.set(
                            numberOfCycles = hist.numberOfCycles + 1,
                            dischargeRate = newRate,
                            currentlyPlugged = plugged,
                        )
                    }
                }

            !plugged ->
                if (hist.currentlyPlugged || level > hist.unpluggedLevel) {
                    val nowUnplug = Clock.System.now()
                    Log.v(Const.TAG, "History aircharging at $level > ${hist.unpluggedLevel}")
                    
                    savedHistory.edit {
                        putLong(Const.HIST_UNPLUG_TIME, nowUnplug.epochSeconds)
                        putInt(Const.HIST_UNPLUG_LEVEL, level)
                        putBoolean(Const.HIST_PLUG_STATE, plugged)
                    }
    
                    historyFlow.value = hist.set(
                        lastUnplug = nowUnplug,
                        unpluggedLevel = level,
                        currentlyPlugged = plugged,
                    )
                    
                } else {
                    val discharge = hist.unpluggedLevel - level
                    val duration = Clock.System.now() - hist.lastUnplug
                    if (discharge >= 10 && duration.inWholeSeconds > 3600) {
                        val inDays = duration.inWholeSeconds.toFloat() / (3600 * 24)
                        val dischargeRate = discharge.toFloat() / inDays
                        historyFlow.value = hist.set(
                            dischargeRate =
                                if (historyFlow.value.numberOfCycles > 0)
                                    (historyFlow.value.dischargeRate + dischargeRate) / 2f
                                else dischargeRate
                        )
                    }
                }
        }
        
        if (hist.currentlyPlugged != plugged) {
            savedHistory.edit {
                putBoolean(Const.HIST_PLUG_STATE, plugged)
            }
            Log.v(Const.TAG, "History plugged=$plugged")
            historyFlow.value = historyFlow.value.set(currentlyPlugged = plugged)
        }
    }

    fun clear() {
        savedHistory.edit {
            putInt(Const.HIST_N_CYCLES, 0)
            putFloat(Const.HIST_DISCHG_RATE, 1f)
            putLong(Const.HIST_INIT_DATE, Instant.DISTANT_PAST.epochSeconds)
        }
        historyFlow.value = historyFlow.value.set(
            initDate = Instant.DISTANT_PAST,
            numberOfCycles = 0,
            dischargeRate = 0f,
        )
    }
}
