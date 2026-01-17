package name.jayhan.pebble

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.isDistantPast

data class HistoryData(
    var isValid: Boolean = false,
    var numberOfCycles: Int = 0,
    var dischargeRate: Float = 1.0f,
)

object History {
    val historyFlow = MutableStateFlow(HistoryData())
    private lateinit var savedHistory: SharedPreferences

    fun init(context: Context) {
        savedHistory = context.getSharedPreferences(
            AppConstants.PREF_HISTORY,
            Context.MODE_PRIVATE
        )
        val numberOfCycles = savedHistory.getInt(AppConstants.HIST_N_CYCLES_I, 0)
        if (numberOfCycles > 0) {
            val dischargeRate = savedHistory.getFloat(AppConstants.HIST_DISCHG_RATE_F, 0f)
            if (dischargeRate != 0f) {
                historyFlow.value = HistoryData(
                    true,
                    numberOfCycles,
                    dischargeRate
                )
            }
        }
    }

    var unpluggedTime: Instant = Instant.DISTANT_PAST
    var currentlyPlugged: Boolean = false
    var unpluggedLevel: Int = 0

    fun event(
        level: Int,
        plugged: Boolean
    ) {
        if (plugged && !currentlyPlugged) {
            if (!unpluggedTime.isDistantPast) {
                val discharge = unpluggedLevel - level
                val duration = Clock.System.now() - unpluggedTime
                if (discharge > 0 && duration.inWholeSeconds > 3600) {
                    val inDays = duration.inWholeSeconds.toFloat() / (3600 * 24)
                    val dischargeRate = discharge.toFloat() / inDays
                    val numberOfCycles = historyFlow.value.numberOfCycles
                    val newRate = (historyFlow.value.dischargeRate * numberOfCycles + dischargeRate) /
                            (numberOfCycles + 1)
                    historyFlow.value = HistoryData(
                        true,
                        numberOfCycles,
                        newRate
                    )
                }
            }
        }
        if (currentlyPlugged && !plugged) {
            unpluggedTime = Clock.System.now()
            unpluggedLevel = level
        }

        currentlyPlugged = plugged
    }
}
