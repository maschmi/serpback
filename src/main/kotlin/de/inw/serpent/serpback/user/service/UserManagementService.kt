package de.inw.serpent.serpback.user.service

import de.inw.serpent.serpback.user.PasswordResetTokenRepository
import de.inw.serpent.serpback.user.UserAuthoritiesRepository
import de.inw.serpent.serpback.user.UserRepository
import de.inw.serpent.serpback.user.domain.PasswordResetToken
import de.inw.serpent.serpback.user.domain.User
import de.inw.serpent.serpback.user.domain.mapToDto
import de.inw.serpent.serpback.user.dto.UserDto
import de.inw.serpent.serpback.user.events.PasswordResetEvent
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.*
import javax.transaction.Transactional

@Service
@Transactional
class UserManagementService(private val userRepository: UserRepository,
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
    }

    private fun getUserEntityByLoginOrMail(username: String): User? {
        return userRepository.findByLogin(username)
            ?: userRepository.findByEmail(username)
    }

    private fun deleteExistingResetToken(login: String) {
        val existingToken = resetTokenRepository.findByLogin(login)
        if (existingToken != null) {
            resetTokenRepository.delete(existingToken)
        }
    }

    fun finishResetPassword(token: String, password: String) {
        val user = resetTokenRepository
            .findByToken(token)?.user
            ?: return
        user.password = passwordEncoder.encode(password)
        userRepository.save(user)
        resetTokenRepository.deleteByToken(token)
    }

    fun tryDeleteUser(username: String): Boolean {
        val uid = userRepository.findByLogin(username)?.id ?: return true
        userRepository.deleteById(uid)
        return true
    }

    fun createUser(user: UserDto, authorities: List<String>): UserDto {
        val userEntity = User(
            user.firstName,
            user.lastName,
            user.login,
            user.email,
            passwordEncoder.encode(UUID.randomUUID().toString()),
            true,
            authorities.mapNotNull { a -> userAuthoritiesRepository.findByAuthority(a) }
        )
        val savedUser = userRepository.save(userEntity)
        passwordResetInit(user.login)
        return savedUser.mapToDto()
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
        resetTokenRepository
            .findAll()
            .filter { token ->  !isResetTokenValid(token)}
            .forEach { token -> resetTokenRepository.delete(token) }
    }

    private fun calculateExpiryDate(): Date {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, RESET_EXPIRATION_IN_MIN)
        return cal.time
    }

    private fun isResetTokenValid(token: PasswordResetToken) =
        token.expirationDate?.after(Date.from(Calendar.getInstance().toInstant())) ?: false
}
