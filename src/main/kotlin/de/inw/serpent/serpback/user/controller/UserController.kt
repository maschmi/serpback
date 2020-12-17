package de.inw.serpent.serpback.user.controller

import de.inw.serpent.serpback.type.ErrorResult
import de.inw.serpent.serpback.user.controller.exception.UserConfirmationException
import de.inw.serpent.serpback.user.controller.exception.UserRegistrationException
import de.inw.serpent.serpback.user.dto.*
import de.inw.serpent.serpback.user.service.UserLoginService
import de.inw.serpent.serpback.user.service.UserManagementService
import de.inw.serpent.serpback.user.service.UserCreationService
import de.inw.serpent.serpback.user.service.UserServiceError
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import javax.validation.Valid

@RestController
@RequestMapping("api/user")
class UserController(val userCreationService: UserCreationService,
                     val userLoginService: UserLoginService,
                     val userManagementService: UserManagementService
                     ) {

    @GetMapping("/register/{token}")
    fun confirmRegistration(@PathVariable token: String): ResponseEntity<Boolean> {
        val result = userCreationService.confirmRegistration(token)
        if(result.isError) {
            throw UserConfirmationException(result.errorOrNull<UserServiceError>() ?: UserServiceError.UNKNOWN_ERROR)
        }
        return ResponseEntity.ok(result.getOrNull<Boolean>() ?: false)
    }

    @PostMapping("/register")
    fun registerUser(@Valid @RequestBody account: UserRegistrationRequest): ResponseEntity<UserCreatedResponse> {
        val newUserResult = userCreationService.registerUser(account)
        return processCreationResult(newUserResult)
    }

    @PostMapping("/login")
    fun loginUser(@Valid @RequestBody user: UserLoginRequest): ResponseEntity<UserLoginResponse> {
        val loginResult = userLoginService.login(user.username, user.password)
        if (loginResult.isError) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        return ResponseEntity.ok().body(loginResult.getOrNull<UserLoginResponse>())
    }

    @PostMapping("/reset/init")
    fun passwordResetInit(@Valid @RequestBody request: UserPasswordResetInitRequest): ResponseEntity<Nothing> {
        userManagementService.passwordResetInit(request.login)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/reset/finish/{token}")
    fun passwordResetFinish(@PathVariable token: String,
        @Valid @RequestBody request: UserPasswordResetFinishRequest): ResponseEntity<Nothing> {
        userManagementService.passwordResetFinish(token, request.password)
        return ResponseEntity.ok().build()
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/create")
    fun createUser(@RequestBody request: UserCreationRequest): ResponseEntity<UserCreatedResponse> {
        val newUserResult = userCreationService.createUser(request)
        return processCreationResult(newUserResult)
    }

    private fun processCreationResult(newUserResult: ErrorResult<UserCreatedResponse, UserServiceError>): ResponseEntity<UserCreatedResponse> {
        if (newUserResult.isError) {
            throw UserRegistrationException(newUserResult.errorOrNull<UserServiceError>()
                ?: UserServiceError.UNKNOWN_ERROR)
        }
        val newUser = newUserResult.getOrNull<UserCreatedResponse>()
            ?: throw UserRegistrationException(UserServiceError.UNKNOWN_ERROR)
        val createdUri = ServletUriComponentsBuilder
            .fromCurrentRequestUri()
            .replacePath("/api/user")
            .path("/{userlogin}").buildAndExpand(newUser.login)
            .toUri()
        return ResponseEntity.created(createdUri).body(newUser)
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
