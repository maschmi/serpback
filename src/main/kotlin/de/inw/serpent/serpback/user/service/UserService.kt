package de.inw.serpent.serpback.user.service

import de.inw.serpent.serpback.type.ErrorResult
import de.inw.serpent.serpback.user.RegistrationTokenRepository
import de.inw.serpent.serpback.user.UserRepository
import de.inw.serpent.serpback.user.domain.RegistrationToken
import de.inw.serpent.serpback.user.domain.User
import de.inw.serpent.serpback.user.domain.mapToDto
import de.inw.serpent.serpback.user.domain.mapToEntity
import de.inw.serpent.serpback.user.dto.AccountDto
import de.inw.serpent.serpback.user.dto.UserDto
import de.inw.serpent.serpback.user.events.UserRegisteredEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.*

@Service
class UserService(private val userRepository: UserRepository,
                  private val registrationTokenRepository: RegistrationTokenRepository,
                  private val passwordEncoder: PasswordEncoder,
                  private val eventPublisher: ApplicationEventPublisher) {

    private val REGISTRATION_EXPIRATION_IN_MIN = 60*24
    private val log = LoggerFactory.getLogger(UserService::class.java)

    fun registerUser(account: AccountDto): ErrorResult<UserDto, UserServiceError> {
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

    private fun calculateExpiryDate(): Date {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, REGISTRATION_EXPIRATION_IN_MIN)
        return cal.time
    }
}



