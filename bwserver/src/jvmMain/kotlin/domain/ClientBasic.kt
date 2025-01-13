package domain

import kotlinx.serialization.Serializable

@Serializable
data class ClientBasic(
    val id: Int,
    val name: String
)
