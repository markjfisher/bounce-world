package extensions

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

inline fun <reified T> serializeObjectToByteArray(obj: T): ByteArray {
    // Explicitly get the serializer for the type T and use it to serialize obj
    val jsonString = Json.encodeToString(obj)

    // Convert the JSON string to a ByteArray
    return jsonString.toByteArray(Charsets.UTF_8)
}

@OptIn(ExperimentalStdlibApi::class)
fun ByteArray.toCustomHexDump(): String {
    val hexFormat = HexFormat {
        upperCase = false
        bytes {
            bytesPerGroup = 1
            groupSeparator = " "
        }
    }
    // Convert the byte array to a hex string using the defined format
    val asHex = this.toHexString(hexFormat)

    // Prepare to build the ASCII representation
    val asciiBuilder = StringBuilder()

    // Split the hex string into chunks of 16 bytes (48 characters, considering spaces)
    val chunks = asHex.chunked(48) // 16 bytes * 3 characters per byte (2 hex digits + space)

    // Map each chunk to its ASCII representation, and format with offset
    val linesWithOffset = chunks.mapIndexed { index, chunk ->
        // For ASCII representation
        val start = index * 16
        val end = minOf(start + 16, this.size)
        asciiBuilder.clear()

        for (i in start until end) {
            val byte = this[i]
            // Append ASCII character or '.' for non-printable bytes
            asciiBuilder.append(if (byte in 32..126) byte.toInt().toChar() else '.')
        }

        // Ensure ASCII representation aligns for lines less than 16 bytes
        val padding = "   ".repeat(16 - (end - start)) // Compensate for the hex part
        val asciiPadding = " ".repeat(16 - asciiBuilder.length) // Compensate for the ASCII part

        String.format("%04x: %s%s |%s%s|", index * 16, chunk.trim(), padding, asciiBuilder.toString(), asciiPadding)
    }

    return linesWithOffset.joinToString("\n")
}