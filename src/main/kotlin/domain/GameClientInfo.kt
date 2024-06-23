package domain

import io.micronaut.serde.annotation.Serdeable

@Serdeable.Deserializable
@Serdeable.Serializable
data class GameClientInfo(
    val name: String,
    val screenSize: ScreenSize
)