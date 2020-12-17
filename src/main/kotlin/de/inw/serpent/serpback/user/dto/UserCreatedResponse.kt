package de.inw.serpent.serpback.user.dto

data class UserCreatedResponse(
    val email: String,
    val login: String,
    val authorities: List<String>
)
