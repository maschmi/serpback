package de.inw.serpent.serpback.user.dto

data class UserCreatedResponse(val email: String,
                               val login: String,
                               val roles: List<String>)
