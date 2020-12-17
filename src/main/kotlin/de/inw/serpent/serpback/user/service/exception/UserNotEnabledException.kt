package de.inw.serpent.serpback.user.service.exception

import org.springframework.security.core.AuthenticationException

class UserNotEnabledException(message: String?, val login: String?) : AuthenticationException(message) {
    constructor(login: String) : this(login, login)
}
