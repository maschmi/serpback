package de.inw.serpent.serpback.user.controller

import de.inw.serpent.serpback.user.controller.exception.InvalidUserRegistrationException
import de.inw.serpent.serpback.user.controller.exception.UserConfirmationException
import de.inw.serpent.serpback.user.controller.exception.UserRegistrationException
import de.inw.serpent.serpback.user.dto.AccountDto
import de.inw.serpent.serpback.user.dto.UserDto
import de.inw.serpent.serpback.user.service.UserService
import de.inw.serpent.serpback.user.service.UserServiceError
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.lang.StringBuilder
import javax.validation.Valid

@RestController
@RequestMapping("api/user")
class UserController(val userService: UserService) {

    @GetMapping("/register/{token}")
    fun confirmRegistration(@PathVariable token: String): ResponseEntity<Boolean> {
        val result = userService.confirmRegistration(token)
        if(result.isError) {
            throw UserConfirmationException(result.errorOrNull<UserServiceError>() ?: UserServiceError.UNKNOWN_ERROR)
        }
        return ResponseEntity.ok(result.getOrNull<Boolean>() ?: false)
    }

    @PostMapping("/register")
    fun registerUser(@Valid @RequestBody account: AccountDto): ResponseEntity<UserDto> {
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

    private fun validateUserValues(account: AccountDto): Boolean {
        val sb = StringBuilder()
        if (account.email.isBlank()) {
            sb.append("email ")
        }
        if (account.firstName.isBlank()) {
            sb.append("firstName ")
        }
        if (account.lastName.isBlank()) {
            sb.append("lastName ")
        }
        if (account.password.isBlank()) {
            sb.append("password ")
        }
        if (account.login.isBlank() || account.login.length <= 3) {
            sb.append("login ")
        }
        val invalidFields = sb.toString()
        if (invalidFields.isBlank()) {
            return true
        }

        throw InvalidUserRegistrationException(invalidFields)
    }

}