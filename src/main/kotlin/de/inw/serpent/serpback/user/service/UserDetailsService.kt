package de.inw.serpent.serpback.user.service

import de.inw.serpent.serpback.user.UserRepository
import de.inw.serpent.serpback.user.dto.UserPrincipal
import de.inw.serpent.serpback.user.service.exception.UserNotEnabledException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsService(val userRepository: UserRepository)
    : UserDetailsService {

    override fun loadUserByUsername(username: String?): UserDetails {
        if (username == null) {
            throw UsernameNotFoundException("")
        }
        val user = userRepository.findByLogin(username)
            ?: throw UsernameNotFoundException(username)
        if (!(user.enabled ?: false)) {
            throw UserNotEnabledException(username)
        }
        return UserPrincipal(user)
    }
}