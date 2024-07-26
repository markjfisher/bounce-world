package domain

import io.micronaut.serde.annotation.Serdeable

@Serdeable.Deserializable
@Serdeable.Serializable
data class GameClientInfo(
    val name: String,
    val version: Int = 1,
    val screenSize: ScreenSize = ScreenSize(0, 0)
)