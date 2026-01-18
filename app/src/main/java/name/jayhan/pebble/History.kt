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
    val numberOfCycles: Int = 0,
    val dischargeRate: Float = 1.0f,
) {
    fun isValid(): Boolean {
        return initDate != Instant.DISTANT_PAST
    }

    fun set(
        initDate: Instant? = null,
        lastUnplug: Instant? = null,
        numberOfCycles: Int? = null,
        dischargeRate: Float? = null
    ): HistoryData {
        return HistoryData(
            initDate ?: this.initDate,
            lastUnplug ?:this.lastUnplug,
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
            AppConstants.PREF_HISTORY,
            Context.MODE_PRIVATE
        )

        val numberOfCycles = savedHistory.getInt(AppConstants.HIST_N_CYCLES, 0)
        val dischargeRate = savedHistory.getFloat(AppConstants.HIST_DISCHG_RATE, 0f)
        val initDate = longToDate(savedHistory.getLong(AppConstants.HIST_INIT_DATE, 0))
        val lastUnplug = longToDate(savedHistory.getLong(AppConstants.HIST_UNPLUG_TIME, 0L))
        Log.v(
            AppConstants.TAG,
            "History init $numberOfCycles since ${initDate.formatDate()} rate=$dischargeRate, " +
                    "unplugged ${lastUnplug.formatDateTime()}"
        )

        if (numberOfCycles > 0) {
            if (dischargeRate != 0f) {
                historyFlow.value = HistoryData(
                    initDate,
                    lastUnplug,
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
        var lastUnplug = longToDate(savedHistory.getLong(AppConstants.HIST_UNPLUG_TIME, 0L))
        var currentlyPlugged = savedHistory.getBoolean(AppConstants.HIST_PLUG_STATE, false)
        var unpluggedLevel = savedHistory.getInt(AppConstants.HIST_UNPLUG_LEVEL, 0)
        // TODO: Sanitation
        Log.v(AppConstants.TAG, "History event ($level,$plugged)" +
                " when ($unpluggedLevel,$currentlyPlugged,$lastUnplug)")

        if (plugged && !currentlyPlugged) {
            if (!lastUnplug.isDistantPast) {
                val discharge = unpluggedLevel - level
                val duration = Clock.System.now() - lastUnplug
                Log.v(AppConstants.TAG, "History cycle $discharge% in ${duration.inWholeSeconds}s")
                if (discharge > 0 && duration.inWholeSeconds > 3600) {
                    val inDays = duration.inWholeSeconds.toFloat() / (3600 * 24)
                    val dischargeRate = discharge.toFloat() / inDays
                    val numberOfCycles = historyFlow.value.numberOfCycles
                    val newRate = (historyFlow.value.dischargeRate * numberOfCycles + dischargeRate) /
                            (numberOfCycles + 1)
                    savedHistory.edit {
                        putInt(AppConstants.HIST_N_CYCLES, numberOfCycles + 1)
                        putFloat(AppConstants.HIST_DISCHG_RATE, newRate)
                        if (numberOfCycles == 0)
                            putLong(AppConstants.HIST_INIT_DATE, lastUnplug.epochSeconds)
                    }
                    Log.v(AppConstants.TAG, "History saved ${numberOfCycles+1} cycles rate=$newRate")

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
                Log.v(AppConstants.TAG, "History discharging $lastUnplug at $unpluggedLevel")
                savedHistory.edit {
                    putLong(AppConstants.HIST_UNPLUG_TIME, lastUnplug.epochSeconds)
                    putInt(AppConstants.HIST_UNPLUG_LEVEL, unpluggedLevel)
                }

                historyFlow.value = historyFlow.value.set(
                    lastUnplug = lastUnplug,
                )
            }
        }

        if (currentlyPlugged != plugged) {
            currentlyPlugged = plugged
            savedHistory.edit {
                putBoolean(AppConstants.HIST_PLUG_STATE, currentlyPlugged)
            }
            Log.v(AppConstants.TAG, "History plugged=$currentlyPlugged")
        }
    }

    fun clear() {
        savedHistory.edit {
            putBoolean(AppConstants.HIST_PLUG_STATE, false)
            putInt(AppConstants.HIST_N_CYCLES, 0)
            putFloat(AppConstants.HIST_DISCHG_RATE, 1.0f)
            putLong(AppConstants.HIST_INIT_DATE, Instant.DISTANT_PAST.epochSeconds)
        }
        historyFlow.value = HistoryData()
    }
}
