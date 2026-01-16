package name.jayhan.pebble

import androidx.core.text.isDigitsOnly
import java.io.File
import java.io.FileReader
import java.io.PrintWriter
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.toKotlinInstant

data class HistoryData(
    var timeBetweenPlugging: Float = 0f,
    var dropBetweenPlugging: Float = 0f,
    var numberOfCycles: Int = 0,
    var dropRate: Float = 0f,
)

object History {
    private lateinit var file: File
    private lateinit var writer: PrintWriter
    private lateinit var reader: FileReader

    fun init(
        file: File
    ) {
        this.file = file
        if (!file.exists()) file.createNewFile()
        writer = PrintWriter(file)
        reader = FileReader(file)
    }

    fun store(
        level: Int,
        plugged: Boolean
    ) {
        // TODO: Don't store everything!
        val now = Clock.System.now()
        writer.print(now.formatDate())
        writer.printf(" %d ", level)
        writer.println(if (plugged) "P" else "U")
        writer.flush()
    }

    fun calculate(): HistoryData {
        Tracker.init()

        reader.reset()
        reader.forEachLine {
            val columns = it.split(' ')
            if (columns.size == 3 && columns[1].isDigitsOnly()) {
                val datetime = AppConstants.dateFormat.parse(columns[0])?.toInstant()?.toKotlinInstant()
                if (datetime != null) {
                    val level = columns[1].toInt()
                    val plugged = columns[2] == "P"
                    Tracker.addEvent(datetime, level, plugged)
                }
            }
        }
        reader.reset()

        return Tracker.condense()
    }
}

private object Tracker {
    var timeUnplugged = Instant.DISTANT_PAST
    var levelUnplugged = 0
    var plugged = false

    fun init() {
        timeUnplugged = Instant.DISTANT_PAST
        levelUnplugged = 0
        plugged = false
        Accumulator.reset()
    }

    fun addEvent(
        datetime: Instant,
        level: Int,
        plug: Boolean,
    ) {
        // TODO: Calculate battery cycle duration and utilization
        Accumulator.add(0f, 0f)
    }

    fun condense(): HistoryData {
        return Accumulator.condense()
    }
}

private object Accumulator {
    private var usageList = mutableListOf<Usage>()

    private data class Usage(
        val durationHours: Float,
        val levelDrop: Float
    )

    fun reset() {
        usageList = mutableListOf()
    }

    fun add(
        durationHours: Float,
        levelDrop: Float,
    ) {
        usageList.add(Usage(durationHours, levelDrop))
    }

    var timeBetweenPlugging = 0f
    var dropBetweenPlugging = 0f
    var numberOfCycles = 0
    var dropRate = 0f

    fun condense(): HistoryData {
        // TODO: Calculate averages
        return HistoryData(
            timeBetweenPlugging,
            dropBetweenPlugging,
            numberOfCycles,
            dropRate,
        )
    }
}