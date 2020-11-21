package de.inw.serpent.serpback.user.dto

import de.inw.serpent.serpback.user.domain.UserAuthorities
import org.springframework.security.core.GrantedAuthority

class Authority(val authorityString: String) : GrantedAuthority {
    override fun getAuthority(): String {
        return authorityString
    }

}