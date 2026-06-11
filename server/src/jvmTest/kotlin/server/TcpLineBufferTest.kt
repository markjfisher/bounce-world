package server

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TcpLineBufferTest {
    private val buffer = TcpLineBuffer()

    @Test
    fun `yields nothing until LF arrives`() {
        buffer.append("x-add-client atari,2,40".toByteArray(), 22) shouldBe emptyList()
    }

    @Test
    fun `reassembles command split across reads`() {
        buffer.append("x-add-client ata".toByteArray(), 14) shouldBe emptyList()
        buffer.append("ri,2,40,22\n".toByteArray(), 12) shouldContainExactly listOf("x-add-client atari,2,40,22")
    }

    @Test
    fun `handles CRLF and multiple lines in one read`() {
        buffer.append("x-shape-count\r\nx-w 3\n".toByteArray(), 22) shouldContainExactly listOf(
            "x-shape-count",
            "x-w 3",
        )
    }
}
