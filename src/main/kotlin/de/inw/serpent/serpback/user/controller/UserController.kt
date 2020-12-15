package de.inw.serpent.serpback.user.controller

import de.inw.serpent.serpback.user.controller.exception.InvalidUserRegistrationException
import de.inw.serpent.serpback.user.controller.exception.UserConfirmationException
import de.inw.serpent.serpback.user.controller.exception.UserRegistrationException
import de.inw.serpent.serpback.user.dto.*
import de.inw.serpent.serpback.user.service.UserService
import de.inw.serpent.serpback.user.service.UserServiceError
import de.inw.serpent.serpback.user.service.exception.UserNotEnabledException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import javax.validation.Valid

@RestController
@RequestMapping("api/user")
class UserController(val userService: UserService, val authManagerBuilder: AuthenticationManagerBuilder) {

    @GetMapping("/register/{token}")
    fun confirmRegistration(@PathVariable token: String): ResponseEntity<Boolean> {
        val result = userService.confirmRegistration(token)
        if(result.isError) {
            throw UserConfirmationException(result.errorOrNull<UserServiceError>() ?: UserServiceError.UNKNOWN_ERROR)
        }
        return ResponseEntity.ok(result.getOrNull<Boolean>() ?: false)
    }

    @PostMapping("/register")
    fun registerUser(@Valid @RequestBody account: AccountInput): ResponseEntity<UserDto> {
        if(validateUserValues(account)) {
            val newUserResult = userService.registerUser(account)
            if (newUserResult.isError) {
                throw UserRegistrationException(newUserResult.errorOrNull<UserServiceError>() ?: UserServiceError.UNKNOWN_ERROR)
            }
            val newUser = newUserResult.getOrNull<UserDto>() ?: throw UserRegistrationException(UserServiceError.UNKNOWN_ERROR)
            val createdUri = ServletUriComponentsBuilder
                .fromCurrentRequestUri()
                .replacePath("/api/user")
                .path("/{userlogin}").buildAndExpand(newUser.login)
                .toUri()
            return ResponseEntity.created(createdUri).body(newUser)
        }
        throw UserRegistrationException(UserServiceError.UNKNOWN_ERROR)
    }

    @PostMapping("/login")
    fun loginUser(@Valid @RequestBody user: UserLoginRequest): ResponseEntity<UserLoginResponse> {

        return try {
            val authToken = UsernamePasswordAuthenticationToken(user.username, user.password)
            val authUser = authenticateUser(authToken, user)
            processAuthentication(authUser)
        } catch(badCredentials: UserBadCredentialsException) {
            val userLogin = userService.getUserloginFromEmail(user.username)
                .getOrNull<String>() ?: throw badCredentials

            val authToken = UsernamePasswordAuthenticationToken(userLogin, user.password)
            val authUser = authenticateUser(authToken, user)
            return processAuthentication(authUser)
        }
    }

    private fun processAuthentication(authUser: Authentication?): ResponseEntity<UserLoginResponse> {
        if (authUser?.isAuthenticated == true) {
            val principal = authUser.principal as UserPrincipal
            SecurityContextHolder.getContext().authentication = authUser
            return ResponseEntity.ok(UserLoginResponse(principal.username))
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
    }

    private fun authenticateUser(
        authToken: UsernamePasswordAuthenticationToken,
        user: UserLoginRequest
    ): Authentication? {
        try {
            return authManagerBuilder.getObject().authenticate(authToken)
        } catch (ex: BadCredentialsException) {
            throw UserBadCredentialsException(user.username)
        } catch (ex: LockedException) {
            throw UserNotEnabledException(user.username)
        } catch (ex: DisabledException) {
            throw UserNotEnabledException(user.username)
        }
    }

    private fun validateUserValues(account: AccountInput): Boolean {
        val invalidFields = ArrayList<String>()
        if (account.email.isBlank()) {
            invalidFields.add("email")
        }
        if (account.password.isBlank()) {
            invalidFields.add("password")
        }
        if (account.login.isBlank() || account.login.length <= 3) {
            invalidFields.add("login")
        }

        if (invalidFields.isEmpty()) {
            return true
        }

        throw InvalidUserRegistrationException(invalidFields)
    }

}