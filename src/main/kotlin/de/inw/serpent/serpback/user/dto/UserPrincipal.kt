package de.inw.serpent.serpback.user.dto

import de.inw.serpent.serpback.user.domain.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.stream.Collectors

class UserPrincipal(val user: User) : UserDetails {
    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        return user.authorities.stream()
            .filter { ua -> ua.authority != null }
            .map { ua -> Authority(ua.authority ?: "") }
            .collect(Collectors.toList())
    }

    override fun getPassword(): String {
        return user.password ?: ""
    }

    override fun getUsername(): String {
        return user.login ?: ""
    }

    override fun isAccountNonExpired(): Boolean {
        return true
    }

    override fun isAccountNonLocked(): Boolean {
        return true
    }

    override fun isCredentialsNonExpired(): Boolean {
        return true
    }

    override fun isEnabled(): Boolean {
        return user.enabled ?: false
    }

}
