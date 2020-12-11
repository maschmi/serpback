package de.inw.serpent.serpback.user.controller.advice

import de.inw.serpent.serpback.user.controller.UserBadCredentialsException
import de.inw.serpent.serpback.user.controller.UserControllerError
import de.inw.serpent.serpback.user.controller.exception.InvalidUserRegistrationException
import de.inw.serpent.serpback.user.controller.exception.UserConfirmationException
import de.inw.serpent.serpback.user.controller.exception.UserRegistrationException
import de.inw.serpent.serpback.user.service.UserServiceError
import de.inw.serpent.serpback.user.service.exception.UserNotEnabledException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.server.ResponseStatusException

@ControllerAdvice
class UserControllerAdvices {

    private val ERROR_HEADER = "errorcode"
    private val ERROR_DETAILS = "detials"

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
        val headers = HttpHeaders()
        headers.add("errorcode", exception.reason.toString())
        headers.add("details", exception.message)
        return when (exception.reason) {
            UserServiceError.USER_ALREADY_CONFIRMED -> returnBadRequest(exception)
            UserServiceError.CONFIRMATION_TIMEOUT -> returnBadRequest(exception)
            else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.reason.toString())
        }

    }

    private fun returnBadRequest(exception: UserConfirmationException) =
        ResponseEntity.badRequest().body(exception.reason.toString())

    @ExceptionHandler()
    fun invalidUserRegistration(exception: InvalidUserRegistrationException): ResponseEntity<String> {
        val headers = HttpHeaders()
        headers.add("errorcode", UserControllerError.INVALID_DATA.toString() )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .headers(headers)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{ \"invalidfields\": ["+ exception.invalidFields.joinToString(",") +"]}")
    }

    @ExceptionHandler()
    fun userNotActivated(exception: UserNotEnabledException): ResponseEntity<String> {
        val headers = HttpHeaders()
        headers.add("errorcode", UserControllerError.USER_NOT_ACTIVATED.toString())
        headers.add("details", exception.login ?: "")
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .headers(headers)
            .body(UserControllerError.USER_NOT_ACTIVATED.toString())
    }

    @ExceptionHandler()
    fun userNotActivated(exception: UserBadCredentialsException): ResponseEntity<String> {
        val headers = HttpHeaders()
        headers.add("errorcode", UserControllerError.USER_BAD_CREDENTIALS.toString())
        headers.add("details", exception.username)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .headers(headers)
            .body(UserControllerError.USER_BAD_CREDENTIALS.toString())
    }

}