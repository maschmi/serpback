package de.inw.serpent.serpback.user.dto

data class UserCreatedResponse(val firstName: String,
                               val lastName: String,
                               val email: String,
                               val login: String,
                               val roles: List<String>)
