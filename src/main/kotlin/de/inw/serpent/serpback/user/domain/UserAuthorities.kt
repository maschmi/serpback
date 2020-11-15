package de.inw.serpent.serpback.user.domain

import de.inw.serpent.serpback.domain.EntityWithId
import javax.persistence.*

@Entity
@Table(name = "user_authorities")
class UserAuthorities(

    @Column(length = 50, nullable = false)
    var authority: String?,

    @ManyToMany(mappedBy = "authorities")
    var users: Set<User>

) : EntityWithId()