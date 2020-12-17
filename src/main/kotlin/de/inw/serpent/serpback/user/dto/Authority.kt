package de.inw.serpent.serpback.user.dto

import org.springframework.security.core.GrantedAuthority

class Authority(val authorityString: String) : GrantedAuthority {
    override fun getAuthority(): String {
        return authorityString
    }

}