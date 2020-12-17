package de.inw.serpent.serpback.user.dto

data class UserLoginResponse(val login: String, val authorities: List<String>) {
}