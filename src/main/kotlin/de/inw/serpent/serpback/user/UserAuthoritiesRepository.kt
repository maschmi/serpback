package de.inw.serpent.serpback.user

import de.inw.serpent.serpback.user.domain.UserAuthorities
import org.springframework.data.jpa.repository.JpaRepository

interface UserAuthoritiesRepository : JpaRepository<UserAuthorities, Long> {
}