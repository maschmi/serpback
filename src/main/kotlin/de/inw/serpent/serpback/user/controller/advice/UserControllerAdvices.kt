package de.inw.serpent.serpback.user.controller.advice

import de.inw.serpent.serpback.user.controller.exception.InvalidUserRegistrationException
import de.inw.serpent.serpback.user.controller.exception.UserConfirmationException
import de.inw.serpent.serpback.user.controller.exception.UserRegistrationException
import de.inw.serpent.serpback.user.service.UserServiceError
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.server.ResponseStatusException

@ControllerAdvice
class UserControllerAdvices {

    @ExceptionHandler()
    fun userNotCreated(exception: UserRegistrationException): ResponseEntity<String> {
        return when (exception.reason) {
            UserServiceError.EMAIL_ALREADY_REGISTERED -> ResponseEntity.badRequest().body(exception.reason.toString())
            UserServiceError.LOGIN_ALREADY_REGISTERED -> ResponseEntity.badRequest().body(exception.reason.toString())
            else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.reason.toString())
        }

    }

    @ExceptionHandler()
    fun userNotConfirmed(exception: UserConfirmationException): ResponseEntity<String> {
        return when (exception.reason) {
            UserServiceError.USER_ALREADY_CONFIRMED -> ResponseEntity.badRequest().body(exception.reason.toString())
            UserServiceError.CONFIRMATION_TIMEOUT -> ResponseEntity.badRequest().body(exception.reason.toString())
            else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.reason.toString())
        }

    }

    @ExceptionHandler()
    fun invalidUserRegistration(exception: InvalidUserRegistrationException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid: " + exception.invalidFields)
    }
}