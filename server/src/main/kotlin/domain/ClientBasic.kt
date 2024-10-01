package domain

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class ClientBasic(
    val id: Int,
    val name: String
)
