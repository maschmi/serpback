package de.inw.serpent.serpback.user.service

import de.inw.serpent.serpback.type.ErrorResult
import de.inw.serpent.serpback.user.UserRepository
import de.inw.serpent.serpback.user.controller.UserBadCredentialsException
import de.inw.serpent.serpback.user.domain.PasswordResetToken
import de.inw.serpent.serpback.user.domain.RegistrationToken
import de.inw.serpent.serpback.user.dto.UserLoginResponse
import de.inw.serpent.serpback.user.dto.UserPrincipal
import de.inw.serpent.serpback.user.service.exception.UserNotEnabledException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.util.*

@Service
class UserLoginService(
    private val userRepository: UserRepository,
    private val authManagerBuilder: AuthenticationManagerBuilder
) {

    fun login(username: String, password: String): ErrorResult<UserLoginResponse, String> {
        return try {
            val authToken = UsernamePasswordAuthenticationToken(username, password)
            val authUser = authenticateUser(authToken, username)
            processAuthentication(authUser)
        } catch (badCredentials: UserBadCredentialsException) {
            val userLogin = getUserLoginFromEmail(username)
                .getOrNull<String>() ?: throw badCredentials

            val authToken = UsernamePasswordAuthenticationToken(userLogin, password)
            val authUser = authenticateUser(authToken, username)
            return processAuthentication(authUser)
        }
    }


    private fun processAuthentication(authUser: Authentication?): ErrorResult<UserLoginResponse, String> {
        if (authUser?.isAuthenticated == true) {
            val principal = authUser.principal as UserPrincipal
            SecurityContextHolder.getContext().authentication = authUser
            return ErrorResult.success(UserLoginResponse(principal.username, principal.authorities.mapNotNull { a -> a.authority }))
        }

        return ErrorResult.failure("")
    }

    private fun authenticateUser(authToken: UsernamePasswordAuthenticationToken,
        username: String): Authentication? {
        try {
            return authManagerBuilder.getObject().authenticate(authToken)
        } catch (ex: BadCredentialsException) {
            throw UserBadCredentialsException(username)
        } catch (ex: LockedException) {
            throw UserNotEnabledException(username)
        } catch (ex: DisabledException) {
            throw UserNotEnabledException(username)
        }
    }

    private fun getUserLoginFromEmail(email: String): ErrorResult<String, UserServiceError> {
        val login = userRepository.findByEmail(email)?.login
        if (login != null) {
            return ErrorResult.success(login)
        }

        return ErrorResult.failure(UserServiceError.UNKNOWN_ERROR)
    }

}
