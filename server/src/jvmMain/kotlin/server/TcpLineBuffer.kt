package server

/**
 * Accumulates TCP stream bytes and yields complete command lines.
 * Line terminators: LF (0x0A), CR (0x0D), and Atari ATASCII EOL (0x9B).
 *
 * Uses ISO-8859-1 decoding so single-byte values like 0x9B are preserved
 * (UTF-8 would replace them with U+FFFD).
 */
internal class TcpLineBuffer {
    private val pending = StringBuilder()

    fun hasPending(): Boolean = pending.isNotEmpty()

    fun pendingDebug(): String = if (pending.isEmpty()) "<empty>" else pending.toString()

    fun append(bytes: ByteArray, length: Int): List<String> {
        if (length <= 0) {
            return emptyList()
        }
        pending.append(String(bytes, 0, length, Charsets.ISO_8859_1))
        return drainCompleteLines()
    }

    private fun drainCompleteLines(): List<String> {
        val lines = mutableListOf<String>()
        while (true) {
            val lineEnd = findLineEnd()
            if (lineEnd < 0) {
                break
            }
            val line = pending.substring(0, lineEnd).trimEnd(*LINE_TERMINATORS).trim()
            pending.delete(0, lineEnd + 1)
            if (line.isNotEmpty()) {
                lines.add(line)
            }
        }
        return lines
    }

    private fun findLineEnd(): Int {
        var earliest = -1
        for (delimiter in LINE_TERMINATORS) {
            val index = pending.indexOf(delimiter)
            if (index >= 0 && (earliest < 0 || index < earliest)) {
                earliest = index
            }
        }
        return earliest
    }

    companion object {
        private val LINE_TERMINATORS = charArrayOf('\n', '\r', '\u009b')
    }
}
