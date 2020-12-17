package de.inw.serpent.serpback.user.service

enum class UserServiceError {
    EMAIL_ALREADY_REGISTERED,
    LOGIN_ALREADY_REGISTERED,
    USER_ALREADY_CONFIRMED,
    CONFIRMATION_TIMEOUT,
    UNKNOWN_ERROR,
    USER_NOT_FOUND
}
