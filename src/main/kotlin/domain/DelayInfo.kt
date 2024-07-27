package domain

import io.micronaut.serde.annotation.Serdeable


@Serdeable.Deserializable
@Serdeable.Serializable
data class DelayInfo(
    val delay: Long
)
