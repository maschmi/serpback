package de.inw.serpent.serpback.user.domain

import de.inw.serpent.serpback.user.dto.AccountInput
import de.inw.serpent.serpback.user.dto.UserDto
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.collections.List as List

fun AccountInput.mapToEntity(passwordEncoder: PasswordEncoder,
                             isEnabled: Boolean = false,
                             authorities: List<UserAuthorities> = ArrayList()
) : User {
    return User(
        this.firstName,
        this.lastName,
        this.login,
        this.email,
        passwordEncoder.encode(this.password),
        isEnabled,
        authorities
    )
}

fun User.mapToDto(): UserDto {
    return UserDto(
        this.firstName ?: "",
        this.lastName ?: "",
        this.email ?: "",
        this.login ?: ""
    )
}
