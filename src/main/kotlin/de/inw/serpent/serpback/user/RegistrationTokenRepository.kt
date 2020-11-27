package de.inw.serpent.serpback.user

import de.inw.serpent.serpback.user.domain.RegistrationToken
import org.springframework.data.jpa.repository.JpaRepository

interface RegistrationTokenRepository : JpaRepository<RegistrationToken, Long> {
}