package de.inw.serpent.serpback.user.service

import de.inw.serpent.serpback.type.ErrorResult
import de.inw.serpent.serpback.user.RegistrationTokenRepository
import de.inw.serpent.serpback.user.UserAuthoritiesRepository
import de.inw.serpent.serpback.user.UserRepository
import de.inw.serpent.serpback.user.service.exception.InvalidUserRegistrationException
import de.inw.serpent.serpback.user.domain.RegistrationToken
import de.inw.serpent.serpback.user.domain.User
import de.inw.serpent.serpback.user.domain.mapToUserCreatedResponse
import de.inw.serpent.serpback.user.domain.mapToEntity
import de.inw.serpent.serpback.user.dto.UserRegistrationRequest
import de.inw.serpent.serpback.user.dto.UserCreatedResponse
import de.inw.serpent.serpback.user.dto.UserCreationRequest
import de.inw.serpent.serpback.user.events.UserRegisteredEvent
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
class UserCreationService(private val userRepository: UserRepository,
                          private val registrationTokenRepository: RegistrationTokenRepository,
                          private val userAuthoritiesRepository: UserAuthoritiesRepository,
                          private val userManagementService: UserManagementService,
                          private val passwordEncoder: PasswordEncoder,
                          private val eventPublisher: ApplicationEventPublisher) {

    private val REGISTRATION_EXPIRATION_IN_MIN = 60*24
    private val log = LoggerFactory.getLogger(UserCreationService::class.java)

    fun createUser(user: UserCreationRequest): ErrorResult<UserCreatedResponse, UserServiceError> {
        validateRequest(user)

        if (userRepository.findByLogin(user.login) != null) {
            return ErrorResult.failure(UserServiceError.LOGIN_ALREADY_REGISTERED)
        }

        if (userRepository.findByEmail(user.email) != null) {
            return ErrorResult.failure(UserServiceError.EMAIL_ALREADY_REGISTERED)
        }

        val userEntity = User(
            user.login,
            user.email,
            passwordEncoder.encode(UUID.randomUUID().toString()),
            true,
            user.authorities?.mapNotNull { a -> userAuthoritiesRepository.findByAuthority(a) } ?: ArrayList()
        )
        val savedUser = userRepository.save(userEntity)
        userManagementService.passwordResetInit(user.login)
        return ErrorResult.success(savedUser.mapToUserCreatedResponse())
    }

    private fun validateRequest(user: UserCreationRequest) {
        val invalidFields = ArrayList<String>()
        if (user.email.isBlank()) {
            invalidFields.add("email")
        }

        if (user.login.isBlank() || user.login.length <= 3) {
            invalidFields.add("login")
        }

        if (invalidFields.isEmpty()) {
            return
        }

        throw InvalidUserRegistrationException(invalidFields)
    }

    fun registerUser(account: UserRegistrationRequest): ErrorResult<UserCreatedResponse, UserServiceError> {
        log.debug("Registering new user {}.", account.login)
        validateRegistrationValues(account)

        if (userRepository.findByLogin(account.login) != null) {
            return ErrorResult.failure(UserServiceError.LOGIN_ALREADY_REGISTERED)
        }

        if (userRepository.findByEmail(account.email) != null) {
            return ErrorResult.failure(UserServiceError.EMAIL_ALREADY_REGISTERED)
        }

        val userEntity = userRepository
            .save(account.mapToEntity(passwordEncoder, false))

        addRegistrationToken(userEntity)
        return ErrorResult.success(userEntity.mapToUserCreatedResponse())
    }

    private fun addRegistrationToken(userEntity: User) {
        val token = UUID.randomUUID().toString()
        val expiryDate = calculateExpiryDate()
        val entity = RegistrationToken(token, userEntity, expiryDate)
        val savedEntity = registrationTokenRepository.save(entity)
        log.debug("Publishing new user registered event for user {}.", userEntity.login)
        eventPublisher.publishEvent(UserRegisteredEvent(this, userEntity.id, savedEntity.token))
    }

    @Scheduled(cron = "0 0 * * * *")
    protected fun removeStaleRegistrationTokens() {
        registrationTokenRepository
            .findAll()
            .filter { registrationToken ->  !isRegistrationTokenValid(registrationToken)}
            .forEach { token -> registrationTokenRepository.delete(token) }
    }

    private fun calculateExpiryDate(): Date {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, REGISTRATION_EXPIRATION_IN_MIN)
        return cal.time
    }

    fun confirmRegistration(token: String): ErrorResult<Boolean, UserServiceError> {
        val registrationToken = registrationTokenRepository.findByToken(token)
            ?: return ErrorResult(UserServiceError.USER_ALREADY_CONFIRMED, false)
        if(isRegistrationTokenValid(registrationToken)) {
            activateUser(registrationToken.user)
            registrationTokenRepository.delete(registrationToken)
            return ErrorResult(true, true)
        } else {
            return ErrorResult(UserServiceError.CONFIRMATION_TIMEOUT, false)
        }
    }

    private fun isRegistrationTokenValid(registrationToken: RegistrationToken) =
        registrationToken.expirationDate.after(Date.from(Calendar.getInstance().toInstant()))

    private fun activateUser(user: User) {
        user.enabled = true
        userRepository.save(user)
    }

    private fun validateRegistrationValues(account: UserRegistrationRequest) {
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
            return
        }

        throw InvalidUserRegistrationException(invalidFields)
    }

}



