package domain

/**
 * Client capabilities, negotiated at registration.
 *
 * The value is an integer written as text (decimal or 0x-prefixed hex) in the registration
 * string — it is never a binary packet field and has no fixed width. Absent or zero means
 * "exact legacy behaviour": 1-byte x/y coordinates and no rotation data.
 * New features must always be additive bits so old clients never change meaning.
 */
object ClientCapabilities {
    // x/y coordinates are little-endian 16-bit values instead of single bytes
    const val WIDE_COORDS = 0x01

    // each shape carries [angle: uint16 LE][angularVelocity: int16 LE] after its coordinates;
    // angle is scaled across a full turn (65535 = 2pi), angular velocity is in 1/256 rad/s
    const val ROTATION = 0x02

    fun has(caps: Int, flag: Int): Boolean = (caps and flag) != 0
}

data class GameClientInfo(
    val name: String,
    val version: Int = 1,
    val screenSize: ScreenSize = ScreenSize(0, 0),
    val worldSize: ScreenSize? = null,
    val capabilities: Int = 0,
)
