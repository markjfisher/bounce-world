package server

/**
 * Accumulates TCP stream bytes and yields complete LF-terminated command lines.
 * Supports CRLF and bare LF; strips surrounding whitespace from each line.
 */
internal class TcpLineBuffer {
    private val pending = StringBuilder()

    fun append(bytes: ByteArray, length: Int): List<String> {
        if (length <= 0) {
            return emptyList()
        }
        pending.append(String(bytes, 0, length, Charsets.UTF_8))
        return drainCompleteLines()
    }

    private fun drainCompleteLines(): List<String> {
        val lines = mutableListOf<String>()
        while (true) {
            val lineEnd = pending.indexOf('\n')
            if (lineEnd < 0) {
                break
            }
            val line = pending.substring(0, lineEnd).trimEnd('\r').trim()
            pending.delete(0, lineEnd + 1)
            if (line.isNotEmpty()) {
                lines.add(line)
            }
        }
        return lines
    }
}
