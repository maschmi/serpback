package de.inw.serpent.serpback.user.domain

import de.inw.serpent.serpback.domain.EntityWithId
import javax.persistence.*


@Entity
@Table(name = "users", indexes = [
    Index(name = "idx_usr_name", columnList = "username")
])
class User(

    @Column(nullable = false, unique = true)
    var username: String? = null,

    @Column(nullable = false)
    var password: String? = null,

    @Column(nullable = false)
    var enabled: Boolean? = true,

    @ManyToMany()
    @JoinTable(
        name = "user_authorites_map",
        joinColumns = [ JoinColumn(name = "user_id", referencedColumnName = "id") ],
        inverseJoinColumns = [ JoinColumn(name = "authority_id", referencedColumnName = "id")]
    )
    var authorities: List<UserAuthorities>
) : EntityWithId()

