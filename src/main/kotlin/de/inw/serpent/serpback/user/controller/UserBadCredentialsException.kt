package de.inw.serpent.serpback.user.controller

import org.springframework.security.core.AuthenticationException

class UserBadCredentialsException(message: String?, val username: String) : AuthenticationException(message) {

    constructor(username: String) : this(username, username)
}
