package de.inw.serpent.serpback.user.domain

import de.inw.serpent.serpback.domain.EntityWithId
import javax.persistence.*

@Entity
@Table(name = "user_authorities")
class UserAuthorities(

    @Column(length = 50, nullable = false)
    var authority: String?,

    @ManyToMany
    @JoinTable(
        name = "user_authorites_map",
        inverseJoinColumns = [JoinColumn(name = "user_id", referencedColumnName = "id")],
        joinColumns = [JoinColumn(name = "authority_id", referencedColumnName = "id")]
    )
    var users: Set<User>

) : EntityWithId()