package de.inw.serpent.serpback.user.dto

data class UserDetailsResponse(val login: String, val email: String, val authorities: List<String>)
