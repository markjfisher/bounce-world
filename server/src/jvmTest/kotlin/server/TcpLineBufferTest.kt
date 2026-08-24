package server

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TcpLineBufferTest {
    private val buffer = TcpLineBuffer()

    @Test
    fun `yields nothing until LF arrives`() {
        val payload = "x-add-client atari,2,40".toByteArray()
        buffer.append(payload, payload.size) shouldBe emptyList()
    }

    @Test
    fun `reassembles command split across reads`() {
        val first = "x-add-client ata".toByteArray()
        val second = "ri,2,40,22\n".toByteArray()
        buffer.append(first, first.size) shouldBe emptyList()
        buffer.append(second, second.size) shouldContainExactly listOf("x-add-client atari,2,40,22")
    }

    @Test
    fun `handles CRLF and multiple lines in one read`() {
        val payload = "x-shape-count\r\nx-w 3\n".toByteArray()
        buffer.append(payload, payload.size) shouldContainExactly listOf(
            "x-shape-count",
            "x-w 3",
        )
    }

    @Test
    fun `handles Atari ATASCII EOL 0x9B as single byte`() {
        val payload = byteArrayOf(
            0x78, 0x2d, 0x73, 0x68, 0x61, 0x70, 0x65, 0x2d, 0x63, 0x6f, 0x75, 0x6e, 0x74, 0x9b.toByte(),
        )
        buffer.append(payload, payload.size) shouldContainExactly listOf("x-shape-count")
    }
}
