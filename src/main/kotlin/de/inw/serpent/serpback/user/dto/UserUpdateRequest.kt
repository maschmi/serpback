package de.inw.serpent.serpback.user.dto

import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

data class UserUpdateRequest(
    @NotBlank val login: String,
    @Email val email: String,
    val authorities: List<String>?
)
