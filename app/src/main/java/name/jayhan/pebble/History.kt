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
    val dischargeRate: Float = 1.0f,
) {
    fun isValid(): Boolean {
        return initDate != Instant.DISTANT_PAST
    }

    fun set(
        initDate: Instant? = null,
        lastUnplug: Instant? = null,
        unpluggedLevel: Int? = null,
        numberOfCycles: Int? = null,
        dischargeRate: Float? = null
    ): HistoryData {
        return HistoryData(
            initDate ?: this.initDate,
            lastUnplug ?:this.lastUnplug,
            unpluggedLevel ?: this.unpluggedLevel,
            numberOfCycles ?: this.numberOfCycles,
            dischargeRate ?: this.dischargeRate
        )
    }
}

object History {
    val historyFlow = MutableStateFlow(HistoryData())
    private lateinit var savedHistory: SharedPreferences

    fun init(context: Context) {
        savedHistory = context.getSharedPreferences(
            AppConst.PREF_HISTORY,
            Context.MODE_PRIVATE
        )

        val numberOfCycles = savedHistory.getInt(AppConst.HIST_N_CYCLES, 0)
        val dischargeRate = savedHistory.getFloat(AppConst.HIST_DISCHG_RATE, 0f)
        val initDate = longToDate(savedHistory.getLong(AppConst.HIST_INIT_DATE, 0))
        val lastUnplug = longToDate(savedHistory.getLong(AppConst.HIST_UNPLUG_TIME, 0L))
        val unpluggedLevel = savedHistory.getInt(AppConst.HIST_UNPLUG_LEVEL, 0)
        Log.v(
            AppConst.TAG,
            "History init $numberOfCycles since ${initDate.formatDate()} rate=$dischargeRate, " +
                    "unplugged ${lastUnplug.formatDateTime()} at $unpluggedLevel%"
        )

        if (numberOfCycles > 0) {
            if (dischargeRate != 0f) {
                historyFlow.value = HistoryData(
                    initDate,
                    lastUnplug,
                    unpluggedLevel,
                    numberOfCycles,
                    dischargeRate
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

    fun event(
        level: Int,
        plugged: Boolean
    ) {
        var lastUnplug = longToDate(savedHistory.getLong(AppConst.HIST_UNPLUG_TIME, 0L))
        var currentlyPlugged = savedHistory.getBoolean(AppConst.HIST_PLUG_STATE, false)
        var unpluggedLevel = savedHistory.getInt(AppConst.HIST_UNPLUG_LEVEL, 0)
        // TODO: Sanitation
        Log.v(AppConst.TAG, "History event ($level,$plugged)" +
                " when ($unpluggedLevel,$currentlyPlugged,$lastUnplug)")

        if (plugged && !currentlyPlugged) {
            if (!lastUnplug.isDistantPast) {
                val discharge = unpluggedLevel - level
                val duration = Clock.System.now() - lastUnplug
                Log.v(AppConst.TAG, "History cycle $discharge% in ${duration.inWholeSeconds}s")
                if (discharge >= 10 && duration.inWholeSeconds > 3600) {
                    val inDays = duration.inWholeSeconds.toFloat() / (3600 * 24)
                    val dischargeRate = discharge.toFloat() / inDays
                    val numberOfCycles = historyFlow.value.numberOfCycles
                    val newRate = (historyFlow.value.dischargeRate * numberOfCycles + dischargeRate) /
                            (numberOfCycles + 1)
                    savedHistory.edit {
                        putInt(AppConst.HIST_N_CYCLES, numberOfCycles + 1)
                        putFloat(AppConst.HIST_DISCHG_RATE, newRate)
                        if (numberOfCycles == 0)
                            putLong(AppConst.HIST_INIT_DATE, lastUnplug.epochSeconds)
                    }
                    Log.v(AppConst.TAG, "History saved ${numberOfCycles+1} cycles rate=$newRate")

                    historyFlow.value = historyFlow.value.set(
                        numberOfCycles = numberOfCycles + 1,
                        dischargeRate = newRate
                    )
                }
            }
        }

        if (!plugged) {
            if (currentlyPlugged || level > unpluggedLevel) {
                lastUnplug = Clock.System.now()
                unpluggedLevel = level
                Log.v(AppConst.TAG, "History discharging $lastUnplug at $unpluggedLevel")
                savedHistory.edit {
                    putLong(AppConst.HIST_UNPLUG_TIME, lastUnplug.epochSeconds)
                    putInt(AppConst.HIST_UNPLUG_LEVEL, unpluggedLevel)
                }

                historyFlow.value = historyFlow.value.set(
                    lastUnplug = lastUnplug,
                )
            }
        }

        if (currentlyPlugged != plugged) {
            currentlyPlugged = plugged
            savedHistory.edit {
                putBoolean(AppConst.HIST_PLUG_STATE, currentlyPlugged)
            }
            Log.v(AppConst.TAG, "History plugged=$currentlyPlugged")
        }
    }

    fun clear() {
        savedHistory.edit {
            putBoolean(AppConst.HIST_PLUG_STATE, false)
            putInt(AppConst.HIST_N_CYCLES, 0)
            putFloat(AppConst.HIST_DISCHG_RATE, 1.0f)
            putLong(AppConst.HIST_INIT_DATE, Instant.DISTANT_PAST.epochSeconds)
        }
        historyFlow.value = HistoryData()
    }
}
