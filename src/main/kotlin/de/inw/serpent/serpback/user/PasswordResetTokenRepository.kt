package de.inw.serpent.serpback.user

import de.inw.serpent.serpback.user.domain.PasswordResetToken
import de.inw.serpent.serpback.user.domain.User
import org.springframework.data.domain.Example
import org.springframework.data.jpa.repository.JpaRepository

interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, Long>, PasswordResetTokenCustomRepository {
    fun findByToken(token: String): PasswordResetToken?
    fun deleteByToken(token: String)
}

