package de.inw.serpent.serpback.user.domain

import de.inw.serpent.serpback.domain.EntityWithId
import java.io.Serializable
import javax.persistence.*


@Entity
@Table(name = "users", indexes = [
    Index(name = "idx_usr_login", columnList = "login"),
    Index(name = "idx_usr_email", columnList = "email")
], uniqueConstraints = [
    UniqueConstraint(name ="uc_user_login", columnNames = ["login"]),
    UniqueConstraint(name ="uc_user_email", columnNames = ["email"])
])
class User(

    @Column(nullable = false, unique = false, name = "first_name")
    var firstName: String? = null,

    @Column(nullable = false, unique = false, name = "last_name")
    var lastName: String? = null,

    @Column(nullable = false, unique = true)
    var login: String? = null,

    @Column(nullable = false, unique = true)
    var email: String? = null,

    @Column(nullable = false)
    var password: String? = null,

    @Column(nullable = false)
    var enabled: Boolean? = false,

    @ManyToMany(cascade = [CascadeType.REMOVE])
    @JoinTable(
        name = "user_authorites_map",
        joinColumns = [ JoinColumn(name = "user_id", referencedColumnName = "id") ],
        inverseJoinColumns = [ JoinColumn(name = "authority_id", referencedColumnName = "id")]
    )
    var authorities: List<UserAuthorities>
) : EntityWithId()

