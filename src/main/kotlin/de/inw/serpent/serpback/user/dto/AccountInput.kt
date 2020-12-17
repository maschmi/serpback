package de.inw.serpent.serpback.user.dto

import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

data class AccountInput(@NotBlank val password: String,
                        @Email val email: String,
                        @NotBlank val login: String)