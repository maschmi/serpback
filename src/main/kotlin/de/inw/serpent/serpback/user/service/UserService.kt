package de.inw.serpent.serpback.user.service

import de.inw.serpent.serpback.type.ErrorResult
import de.inw.serpent.serpback.user.RegistrationTokenRepository
import de.inw.serpent.serpback.user.UserRepository
import de.inw.serpent.serpback.user.domain.RegistrationToken
import de.inw.serpent.serpback.user.domain.User
import de.inw.serpent.serpback.user.domain.mapToDto
import de.inw.serpent.serpback.user.domain.mapToEntity
import de.inw.serpent.serpback.user.dto.AccountInput
import de.inw.serpent.serpback.user.dto.UserDto
import de.inw.serpent.serpback.user.events.UserRegisteredEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.*
import javax.transaction.Transactional

@Service
@Transactional
class UserService(private val userRepository: UserRepository,
                  private val registrationTokenRepository: RegistrationTokenRepository,
                  private val passwordEncoder: PasswordEncoder,
                  private val eventPublisher: ApplicationEventPublisher) {

    private val REGISTRATION_EXPIRATION_IN_MIN = 60*24
    private val log = LoggerFactory.getLogger(UserService::class.java)

    fun registerUser(account: AccountInput): ErrorResult<UserDto, UserServiceError> {
        log.debug("Registering new user {}.", account.login)
        if (userRepository.findByLogin(account.login) != null) {
            return ErrorResult.failure(UserServiceError.LOGIN_ALREADY_REGISTERED)
        }

        if (userRepository.findByEmail(account.email) != null) {
            return ErrorResult.failure(UserServiceError.EMAIL_ALREADY_REGISTERED)
        }

        val userEntity = userRepository
            .save(account.mapToEntity(passwordEncoder, false))

        addRegistrationToken(userEntity)
        return ErrorResult.success(userEntity.mapToDto())
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
}



