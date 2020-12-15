package de.inw.serpent.serpback.user.controller

import de.inw.serpent.serpback.user.controller.exception.UserConfirmationException
import de.inw.serpent.serpback.user.controller.exception.UserRegistrationException
import de.inw.serpent.serpback.user.dto.*
import de.inw.serpent.serpback.user.service.UserLoginService
import de.inw.serpent.serpback.user.service.UserManagementService
import de.inw.serpent.serpback.user.service.UserRegistrationService
import de.inw.serpent.serpback.user.service.UserServiceError
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import javax.validation.Valid

@RestController
@RequestMapping("api/user")
class UserController(val userRegistrationService: UserRegistrationService,
                     val userLoginService: UserLoginService,
                     val userManagementService: UserManagementService
                     ) {

    @GetMapping("/register/{token}")
    fun confirmRegistration(@PathVariable token: String): ResponseEntity<Boolean> {
        val result = userRegistrationService.confirmRegistration(token)
        if(result.isError) {
            throw UserConfirmationException(result.errorOrNull<UserServiceError>() ?: UserServiceError.UNKNOWN_ERROR)
        }
        return ResponseEntity.ok(result.getOrNull<Boolean>() ?: false)
    }

    @PostMapping("/register")
    fun registerUser(@Valid @RequestBody account: AccountInput): ResponseEntity<UserDto> {

            val newUserResult = userRegistrationService.registerUser(account)
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

    @PostMapping("/login")
    fun loginUser(@Valid @RequestBody user: UserLoginRequest): ResponseEntity<UserLoginResponse> {
        val loginResult = userLoginService.login(user.username, user.password)
        if (loginResult.isError) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        return ResponseEntity.ok().body(loginResult.getOrNull<UserLoginResponse>())
    }

    @Secured("ROLE_ADMIN")
    @DeleteMapping("/delete/{login}")
    fun deleteUser(@PathVariable login: String): ResponseEntity<Nothing> {
        if( userManagementService.tryDeleteUser(login)) {
            return ResponseEntity.noContent().build()
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
    }



}
