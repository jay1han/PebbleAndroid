package name.jayhan.dolbom

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.isDistantPast

class HistoryData(
    val historyDate: Instant = Instant.DISTANT_PAST,
    val historyCycles: Int = 0,
    val historyRate: Float = 0f,
    val cycleDate: Instant = Instant.DISTANT_PAST,
    val cycleLevel: Int = 0,
    val cycleRate: Float = 0f,
    val nowPlugged: Boolean = false,
) {
    fun set(
        historyDate: Instant? = null,
        historyCycles: Int? = null,
        historyRate: Float? = null,
        cycleDate: Instant? = null,
        cycleLevel: Int? = null,
        cycleRate: Float? = null,
        nowPlugged: Boolean? = null,
    ): HistoryData {
        return HistoryData(
            historyDate = historyDate ?: this.historyDate,
            historyCycles = historyCycles ?: this.historyCycles,
            historyRate = historyRate ?: this.historyRate,
            cycleDate = cycleDate ?:this.cycleDate,
            cycleLevel = cycleLevel ?: this.cycleLevel,
            cycleRate = cycleRate ?: this.cycleRate,
            nowPlugged = nowPlugged ?: this.nowPlugged,
        )
    }
    
    companion object {
        fun read(prefs: SharedPreferences): HistoryData {
            return HistoryData(
                historyDate = longToDate(prefs.getLong(Const.HIST_INIT_DATE, 0)),
                historyCycles = prefs.getInt(Const.HIST_CYCLES, 0),
                historyRate = prefs.getFloat(Const.HIST_RATE, 0f),
                cycleDate = longToDate(prefs.getLong(Const.HIST_CYCLE_TIME, 0L)),
                cycleLevel = prefs.getInt(Const.HIST_CYCLE_LEVEL, 0),
                cycleRate = prefs.getFloat(Const.CYCLE_RATE, 0f),
                nowPlugged = prefs.getBoolean(Const.HIST_PLUG_STATE, false),
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
            "History init ${historyData.historyCycles} " +
                    "since ${historyData.historyDate.formatDate()} rate=${historyData.historyRate}, " +
                    "unplugged ${historyData.cycleDate.formatDateTime()} at ${historyData.cycleLevel}%"
        )

        historyFlow.value = historyData
    }

    fun event(
        level: Int,
        plugged: Boolean
    ) {
        var hist = HistoryData.read(savedHistory)
        Log.v(Const.TAG, "History event ($level,$plugged)" +
                " now (${hist.cycleLevel},${hist.nowPlugged},${hist.cycleDate.formatDateTime()})")

        val now = Clock.System.now()
        when {
            // Plugging in: recalculate historical rate
            plugged && !hist.nowPlugged ->
                if (!hist.cycleDate.isDistantPast) {
                    val discharge = hist.cycleLevel - level
                    val duration = now - hist.cycleDate
                    Log.v(Const.TAG, "History cycle $discharge% in ${duration.inWholeSeconds}s")
                    if (discharge >= 10 && duration.inWholeSeconds > 3600) {
                        val inDays = duration.inWholeSeconds.toFloat() / (3600 * 24)
                        val dischargeRate = discharge.toFloat() / inDays
                        val newRate =
                            if (hist.historyDate.isDistantPast) dischargeRate
                            else (hist.historyRate * hist.historyCycles + dischargeRate) /
                                    (hist.historyCycles + 1)
                        
                        hist = hist.set(
                            historyCycles = hist.historyCycles + 1,
                            historyRate = newRate,
                            cycleRate = newRate,
                        )
                        
                        savedHistory.edit {
                            if (hist.historyDate.isDistantPast) {
                                hist = hist.set(historyDate = hist.cycleDate)
                                putLong(Const.HIST_INIT_DATE, hist.cycleDate.epochSeconds)
                            }
                            putInt(Const.HIST_CYCLES, hist.historyCycles)
                            putFloat(Const.HIST_RATE, hist.historyRate)
                            putFloat(Const.CYCLE_RATE, hist.cycleRate)
                            commit()
                        }
                        Log.v(Const.TAG, "History saved ${hist.historyCycles+1} cycles rate=$newRate")
    
                        historyFlow.value = hist
                    }
                }

            !plugged ->
                // Unplugged, re-starting cycle
                if (hist.nowPlugged || level > hist.cycleLevel) {
                    Log.v(Const.TAG, "History unplugged at $level > ${hist.cycleLevel}")
                    
                    hist = hist.set(
                        cycleDate = now,
                        cycleLevel = level,
                    )
                    
                    savedHistory.edit {
                        putLong(Const.HIST_CYCLE_TIME, hist.cycleDate.epochSeconds)
                        putInt(Const.HIST_CYCLE_LEVEL, hist.cycleLevel)
                        commit()
                    }
    
                    historyFlow.value = hist
                }
                
                // Continuing unplugged state
                else {
                    val discharge = hist.cycleLevel - level
                    val duration = now - hist.cycleDate
                    if (discharge >= 10 && duration.inWholeSeconds > 3600) {
                        val inDays = duration.inWholeSeconds.toFloat() / (3600 * 24)
                        val dischargeRate = discharge.toFloat() / inDays
                        
                        hist = hist.set(
                            cycleRate =
                                if (historyFlow.value.historyCycles > 0)
                                    (historyFlow.value.historyRate + dischargeRate) / 2f
                                else dischargeRate
                        )
                        
                        savedHistory.edit {
                            putFloat(Const.CYCLE_RATE, hist.cycleRate)
                            commit()
                        }
                        
                        historyFlow.value = hist
                    }
                }
        }
        
        // Changed plugged state
        if (hist.nowPlugged != plugged) {
            Log.v(Const.TAG, "History plugged=$plugged")
            hist = hist.set(nowPlugged = plugged)
            savedHistory.edit {
                putBoolean(Const.HIST_PLUG_STATE, hist.nowPlugged)
                commit()
            }
            historyFlow.value = hist
        }
    }

    fun clear() {
        savedHistory.edit {
            putInt(Const.HIST_CYCLES, 0)
            putFloat(Const.HIST_RATE, 1f)
            putLong(Const.HIST_INIT_DATE, Instant.DISTANT_PAST.epochSeconds)
            commit()
        }
        historyFlow.value = historyFlow.value.set(
            historyDate = Instant.DISTANT_PAST,
            historyCycles = 0,
            historyRate = 0f,
        )
    }
}
