package de.inw.serpent.serpback.user

import de.inw.serpent.serpback.user.domain.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByUsername(username: String): User
}