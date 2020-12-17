package de.inw.serpent.serpback.user

import de.inw.serpent.serpback.user.domain.PasswordResetToken
import de.inw.serpent.serpback.user.domain.User
import org.springframework.data.domain.Example

interface PasswordResetTokenCustomRepository {
    fun findByLogin(login: String): PasswordResetToken?
}

class PasswordResetTokenCustomRepositoryImpl(private val passwordResetTokenRepository: PasswordResetTokenRepository) :
    PasswordResetTokenCustomRepository {
    override fun findByLogin(login: String): PasswordResetToken? {
        val user = User(login, null, null, null, ArrayList())
        val token = PasswordResetToken(null, user, null)
        return passwordResetTokenRepository.findOne(Example.of(token)).orElse(null)
    }

}