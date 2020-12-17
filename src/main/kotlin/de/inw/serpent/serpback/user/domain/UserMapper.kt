package de.inw.serpent.serpback.user.domain

import de.inw.serpent.serpback.user.dto.UserRegistrationRequest
import de.inw.serpent.serpback.user.dto.UserCreatedResponse
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.collections.List as List

fun UserRegistrationRequest.mapToEntity(passwordEncoder: PasswordEncoder,
                                        isEnabled: Boolean = false,
                                        authorities: List<UserAuthorities> = ArrayList()
) : User {
    return User(
        this.login,
        this.email,
        passwordEncoder.encode(this.password),
        isEnabled,
        authorities
    )
}

fun User.mapToUserCreatedResponse(): UserCreatedResponse {
    return UserCreatedResponse(
        this.email ?: "",
        this.login ?: "",
        this.authorities.mapNotNull { a -> a.authority }
    )
}
