package de.inw.serpent.serpback.user.domain

import de.inw.serpent.serpback.user.dto.UserCreatedResponse
import de.inw.serpent.serpback.user.dto.UserDetailsResponse
import de.inw.serpent.serpback.user.dto.UserRegistrationRequest
import org.springframework.security.crypto.password.PasswordEncoder

fun UserRegistrationRequest.mapToEntity(
    passwordEncoder: PasswordEncoder,
    isEnabled: Boolean = false,
    authorities: List<UserAuthorities> = ArrayList()
): User {
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

fun User.mapToUserDetailsResponse(): UserDetailsResponse {
    return UserDetailsResponse(
        this.login ?: "",
        this.email ?: "",
        this.authorities.mapNotNull { a -> a.authority }
    )
}
