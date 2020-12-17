package de.inw.serpent.serpback.user.controller

import de.inw.serpent.serpback.type.ErrorResult
import de.inw.serpent.serpback.user.controller.exception.UserConfirmationException
import de.inw.serpent.serpback.user.controller.exception.UserRegistrationException
import de.inw.serpent.serpback.user.controller.exception.UserUpdateException
import de.inw.serpent.serpback.user.dto.*
import de.inw.serpent.serpback.user.service.UserCreationService
import de.inw.serpent.serpback.user.service.UserLoginService
import de.inw.serpent.serpback.user.service.UserManagementService
import de.inw.serpent.serpback.user.service.UserServiceError
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import javax.validation.Valid

@RestController
@RequestMapping("api/user")
class UserController(
    val userCreationService: UserCreationService,
    val userLoginService: UserLoginService,
    val userManagementService: UserManagementService
) {

    @GetMapping("/register/{token}")
    fun confirmRegistration(@PathVariable token: String): ResponseEntity<Boolean> {
        val result = userCreationService.confirmRegistration(token)
        if (result.isError) {
            throw UserConfirmationException(result.errorOrNull<UserServiceError>() ?: UserServiceError.UNKNOWN_ERROR)
        }
        return ResponseEntity.ok(result.getOrNull<Boolean>() ?: false)
    }

    @GetMapping("/{login}")
    fun getDetails(@PathVariable login: String): ResponseEntity<UserDetailsResponse> {
        val principal = getCurrentUserPrincipal()
            ?: return ResponseEntity.badRequest().build()
        val userIsAdmin = hasPrincipalAdminPrivileges(principal)
        val userInPathIsLoggedInUser = principal.user.login == login
        if (!userInPathIsLoggedInUser && !userIsAdmin) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        val result = userManagementService.getUserDetails(login)
        val details = result.getOrNull<UserDetailsResponse>()
            ?: return calculateErrorToReturn(result)
        return ResponseEntity.ok(details)
    }

    @Secured("ROLE_ADMIN")
    @GetMapping
    fun getAllUserDetails(): ResponseEntity<List<UserDetailsResponse>> {
        return ResponseEntity.ok(userManagementService.getAllUserDetails())
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
    fun passwordResetFinish(
        @PathVariable token: String,
        @Valid @RequestBody request: UserPasswordResetFinishRequest
    ): ResponseEntity<Nothing> {
        userManagementService.passwordResetFinish(token, request.password)
        return ResponseEntity.ok().build()
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/create")
    fun createUser(@Valid @RequestBody request: UserCreationRequest): ResponseEntity<UserCreatedResponse> {
        val newUserResult = userCreationService.createUser(request)
        return processCreationResult(newUserResult)
    }

    @Secured("ROLE_ADMIN")
    @PutMapping("/{login}/authorities")
    fun updateUserAuthorities(
        @PathVariable login: String,
        @Valid @RequestBody request: UserAuthoritiesUpdateRequest
    ): ResponseEntity<UserDetailsResponse> {
        val updatedUser = userManagementService.updateUserAuthorities(login, request)
        val userDetails = updatedUser.getOrNull<UserDetailsResponse>() ?: return calculateErrorToReturn(updatedUser)
        return ResponseEntity.ok(userDetails)
    }

    @PutMapping("/{login}")
    fun updateUser(
        @PathVariable login: String,
        @Valid @RequestBody request: UserUpdateRequest
    ): ResponseEntity<UserDetailsResponse> {
        val principal = getCurrentUserPrincipal() ?: return ResponseEntity.badRequest().build()
        val userIsAdmin = hasPrincipalAdminPrivileges(principal)
        val userInPathIsLoggedInUser = principal.user.login == login
        if (userIsAdmin) {
            val updatedUser = userManagementService.updateUserWithAuthorities(login, request)
            return processUserUpdate(updatedUser)
        }
        if (!userInPathIsLoggedInUser) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val updatedUser = userManagementService.updateUserWithoutAuthorities(login, request)
        return processUserUpdate(updatedUser)
    }

    private fun getCurrentUserPrincipal() =
        SecurityContextHolder.getContext().authentication.principal as UserPrincipal?

    private fun hasPrincipalAdminPrivileges(principal: UserPrincipal) =
        principal.authorities.mapNotNull { a -> a.authority }.contains("ROLE_ADMIN")

    private fun processUserUpdate(updatedUser: ErrorResult<UserDetailsResponse, UserServiceError>): ResponseEntity<UserDetailsResponse> {
        val userDetails = updatedUser.getOrNull<UserDetailsResponse>()
            ?: return calculateErrorToReturn(updatedUser)
        return ResponseEntity.ok(userDetails)
    }

    private fun calculateErrorToReturn(updatedUser: ErrorResult<UserDetailsResponse, UserServiceError>): ResponseEntity<UserDetailsResponse> {
        return when (val error = updatedUser.errorOrNull<UserServiceError>()) {
            UserServiceError.USER_NOT_FOUND -> ResponseEntity.notFound().build()
            UserServiceError.EMAIL_ALREADY_REGISTERED,
            UserServiceError.LOGIN_ALREADY_REGISTERED -> throw UserUpdateException(error)
            else -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    private fun processCreationResult(newUserResult: ErrorResult<UserCreatedResponse, UserServiceError>): ResponseEntity<UserCreatedResponse> {
        if (newUserResult.isError) {
            throw UserRegistrationException(
                newUserResult.errorOrNull<UserServiceError>()
                    ?: UserServiceError.UNKNOWN_ERROR
            )
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
        if (userManagementService.tryDeleteUser(login)) {
            return ResponseEntity.noContent().build()
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
    }
}
