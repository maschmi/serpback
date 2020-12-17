package de.inw.serpent.serpback.user.domain

import de.inw.serpent.serpback.domain.EntityWithId
import java.util.*
import javax.persistence.*


@Entity
@Table(
    name = "registration_tokens",
    indexes = [Index(name = "idx_reg_reset_token", columnList = "token")],
    uniqueConstraints = [UniqueConstraint(name = "uc_reg_reset_token", columnNames = ["token"])]
)
class RegistrationToken(

    @Column(name = "token", nullable = false)
    var token: String,

    @OneToOne(targetEntity = User::class, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)

    var user: User,

    var expirationDate: Date
) : EntityWithId()
