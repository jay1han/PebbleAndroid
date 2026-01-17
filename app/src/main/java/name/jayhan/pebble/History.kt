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
    var initDate: Instant = Instant.DISTANT_PAST,
    var numberOfCycles: Int = 0,
    var dischargeRate: Float = 1.0f,
) {
    fun isValid(): Boolean {
        return initDate != Instant.DISTANT_PAST
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
        val initDate = readInitDate()
        Log.v(AppConstants.TAG, "History init ${numberOfCycles} since ${initDate.formatDate()} rate=$dischargeRate")

        if (numberOfCycles > 0) {
            if (dischargeRate != 0f) {
                historyFlow.value = HistoryData(
                    initDate,
                    numberOfCycles,
                    dischargeRate
                )
            }
        }
    }

    private fun readInitDate(): Instant {
        val initDateLong = savedHistory.getLong(AppConstants.HIST_INIT_DATE, 0)
        val initDate =
            if (initDateLong == 0L) Instant.DISTANT_PAST
            else Instant.fromEpochSeconds(initDateLong)
        return initDate
    }

    fun event(
        level: Int,
        plugged: Boolean
    ) {
        val timeLong = savedHistory.getLong(AppConstants.HIST_UNPLUG_TIME, 0L)
        var unpluggedTime =
            if (timeLong == 0L) Instant.DISTANT_PAST
            else Instant.fromEpochSeconds(timeLong)
        var currentlyPlugged = savedHistory.getBoolean(AppConstants.HIST_PLUG_STATE, false)
        var unpluggedLevel = savedHistory.getInt(AppConstants.HIST_UNPLUG_LEVEL, 0)
        // TODO: Sanitation
        Log.v(AppConstants.TAG, "History event ($level,$plugged)" +
                " when ($unpluggedLevel,$currentlyPlugged,$unpluggedTime)")

        if (plugged && !currentlyPlugged) {
            if (!unpluggedTime.isDistantPast) {
                val discharge = unpluggedLevel - level
                val duration = Clock.System.now() - unpluggedTime
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
                            putLong(AppConstants.HIST_INIT_DATE, unpluggedTime.epochSeconds)
                    }
                    Log.v(AppConstants.TAG, "History saved ${numberOfCycles+1} cycles rate=$newRate")

                    historyFlow.value = HistoryData(
                        readInitDate(),
                        numberOfCycles + 1,
                        newRate
                    )
                }
            }
        }
        if (currentlyPlugged && !plugged) {
            unpluggedTime = Clock.System.now()
            unpluggedLevel = level
            savedHistory.edit {
                putLong(AppConstants.HIST_UNPLUG_TIME, unpluggedTime.epochSeconds)
                putInt(AppConstants.HIST_UNPLUG_LEVEL, unpluggedLevel)
            }
            Log.v(AppConstants.TAG, "History unplugged $unpluggedTime at $unpluggedLevel")
        }

        if (currentlyPlugged != plugged) {
            currentlyPlugged = plugged
            savedHistory.edit {
                putBoolean(AppConstants.HIST_PLUG_STATE, currentlyPlugged)
                Log.v(AppConstants.TAG, "History plugged=$currentlyPlugged")
            }
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
