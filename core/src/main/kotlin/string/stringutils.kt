package string

import kotlin.time.TimeSource

fun formatUptime(startTime: TimeSource.Monotonic.ValueTimeMark, endTime: TimeSource.Monotonic.ValueTimeMark): String {
    val elapsed = endTime - startTime
    return elapsed.toComponents { h, m, _, _ ->
        buildString {
            val d = elapsed.inWholeDays
            if (d > 0) {
                append("$d day")
                if (d != 1L) append("s")
                append(", ")
            }
            if (h > 0 || d > 0) {
                val partHours = h - d * 24
                append("$partHours hour")
                if (partHours != 1L) append("s")
                append(", ")
            }
            append("$m min")
            if (m != 1) append("s")
        }
    }
}