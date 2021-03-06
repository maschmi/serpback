package de.inw.serpent.serpback.user.service

import de.inw.serpent.serpback.type.ErrorResult
import de.inw.serpent.serpback.user.PasswordResetTokenRepository
import de.inw.serpent.serpback.user.UserAuthoritiesRepository
import de.inw.serpent.serpback.user.UserRepository
import de.inw.serpent.serpback.user.domain.PasswordResetToken
import de.inw.serpent.serpback.user.domain.User
import de.inw.serpent.serpback.user.domain.UserAuthorities
import de.inw.serpent.serpback.user.domain.mapToUserDetailsResponse
import de.inw.serpent.serpback.user.dto.UserAuthoritiesUpdateRequest
import de.inw.serpent.serpback.user.dto.UserDetailsResponse
import de.inw.serpent.serpback.user.dto.UserUpdateRequest
import de.inw.serpent.serpback.user.events.PasswordResetEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.*
import javax.transaction.Transactional
import kotlin.collections.ArrayList

@Service
@Transactional
class UserManagementService(
    private val userRepository: UserRepository,
    private val userAuthoritiesRepository: UserAuthoritiesRepository,
    private val resetTokenRepository: PasswordResetTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val eventPublisher: ApplicationEventPublisher
) {

    private val RESET_EXPIRATION_IN_MIN = 120
    private val log = LoggerFactory.getLogger(UserManagementService::class.java)

    fun passwordResetInit(username: String) {
        val userEntity = getUserEntityByLoginOrMail(username) ?: return
        deleteExistingResetToken(userEntity.login ?: "")
        createResetToken(userEntity)
        log.debug("Password reset for uid ${userEntity.id} initiated.")
    }

    private fun getUserEntityByLoginOrMail(username: String): User? {
        return userRepository.findByLogin(username)
            ?: userRepository.findByEmail(username)
    }

    private fun deleteExistingResetToken(login: String) {

        val existingToken = resetTokenRepository.findByLogin(login)
        if (existingToken != null) {
            log.debug("Deleting existing reset token ${existingToken.token} for user login $login")
            resetTokenRepository.delete(existingToken)
        }
    }

    fun passwordResetFinish(token: String, password: String) {
        val user = resetTokenRepository
            .findByToken(token)?.user
            ?: return
        user.password = passwordEncoder.encode(password)
        userRepository.save(user)
        resetTokenRepository.deleteByToken(token)
        log.debug("Password reset for with token $token for uid ${user.id} finished")
    }

    fun tryDeleteUser(username: String): Boolean {
        val uid = userRepository.findByLogin(username)?.id ?: return true
        userRepository.deleteById(uid)
        return true
    }

    fun updateUserAuthorities(
        login: String,
        request: UserAuthoritiesUpdateRequest
    ): ErrorResult<UserDetailsResponse, UserServiceError> {
        val userEntity = userRepository.findByLogin(login)
            ?: return ErrorResult.failure(UserServiceError.USER_NOT_FOUND)
        val mappedAuthorities = mapRequestAuthoritiesToEntities(request.authorities ?: ArrayList())
        userEntity.authorities = mappedAuthorities
        val savedEntity = userRepository.save(userEntity)
        return ErrorResult.success(savedEntity.mapToUserDetailsResponse())
    }

    private fun mapRequestAuthoritiesToEntities(authorities: List<String>): List<UserAuthorities> {
        return authorities.mapNotNull { a -> userAuthoritiesRepository.findByAuthority(a) }
    }

    private fun createResetToken(userEntity: User) {
        val token = UUID.randomUUID().toString()
        val expiryDate = calculateExpiryDate()
        val entity = PasswordResetToken(token, userEntity, expiryDate)
        val savedEntity = resetTokenRepository.save(entity)
        log.debug("Publishing new user password reset event for user {}.", userEntity.login)
        eventPublisher.publishEvent(PasswordResetEvent(this, userEntity.id, savedEntity.token ?: ""))
    }

    @Scheduled(cron = "0 0 * * * *")
    protected fun removeStaleResetTokens() {
        val staleRestTokens = resetTokenRepository
            .findAll()
            .filter { token -> !isResetTokenValid(token) }
        log.debug("Removing stale reset tokens {}", staleRestTokens.map { t -> "$t.token, $t.expirationDate }" })
        staleRestTokens
            .forEach { token -> resetTokenRepository.delete(token) }
    }

    private fun calculateExpiryDate(): Date {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, RESET_EXPIRATION_IN_MIN)
        return cal.time
    }

    private fun isResetTokenValid(token: PasswordResetToken) =
        token.expirationDate?.after(Date.from(Calendar.getInstance().toInstant())) ?: false

    fun updateUserWithAuthorities(
        login: String,
        request: UserUpdateRequest
    ): ErrorResult<UserDetailsResponse, UserServiceError> {
        return updateUser(login, request, true)
    }

    fun updateUserWithoutAuthorities(
        login: String,
        request: UserUpdateRequest
    ): ErrorResult<UserDetailsResponse, UserServiceError> {
        return updateUser(login, request, false)
    }

    private fun updateUser(
        login: String,
        request: UserUpdateRequest,
        isAdminMode: Boolean
    ): ErrorResult<UserDetailsResponse, UserServiceError> {
        val verificationResult = verifyNewLoginAndMailAreNotTaken(login, request)
        if (verificationResult.isError) {
            return ErrorResult.failure(
                verificationResult.errorOrNull<UserServiceError>() ?: UserServiceError.UNKNOWN_ERROR
            )
        }

        val userEntity =
            userRepository.findByLogin(login) ?: return ErrorResult.failure(UserServiceError.USER_NOT_FOUND)
        userEntity.login = request.login
        userEntity.email = request.email
        if (isAdminMode) {
            userEntity.authorities = mapRequestAuthoritiesToEntities(request.authorities ?: ArrayList())
        }
        val savedUser = userRepository.save(userEntity)
        return ErrorResult.success(savedUser.mapToUserDetailsResponse())
    }

    private fun verifyNewLoginAndMailAreNotTaken(
        login: String,
        request: UserUpdateRequest
    ): ErrorResult<UserUpdateRequest, UserServiceError> {
        if (login != request.login && userRepository.findByLogin(request.login) != null) {
            return ErrorResult.failure(UserServiceError.LOGIN_ALREADY_REGISTERED)
        }

        if (userRepository.findByEmail(request.email)?.login != login) {
            return ErrorResult.failure(UserServiceError.EMAIL_ALREADY_REGISTERED)
        }
        return ErrorResult.success(request)
    }

    fun getUserDetails(login: String): ErrorResult<UserDetailsResponse, UserServiceError> {
        val details = userRepository.findByLogin(login)?.mapToUserDetailsResponse()
            ?: return ErrorResult.failure(UserServiceError.USER_NOT_FOUND)
        return ErrorResult.success(details)
    }

    fun getAllUserDetails(): List<UserDetailsResponse> {
        return userRepository
            .findAll()
            .mapNotNull { it.mapToUserDetailsResponse() }
    }
}
