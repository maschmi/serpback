package de.inw.serpent.serpback.user.domain


import de.inw.serpent.serpback.domain.EntityWithId
import java.util.*
import javax.persistence.*

@Entity
@Table(
    name = "password_reset_tokens",
    indexes = [Index(name = "idx_pw_reset_token", columnList = "token")],
    uniqueConstraints = [UniqueConstraint(name = "uc_pw_reset_token", columnNames = ["token"])]
)
class PasswordResetToken(

    @Column(name = "token", nullable = false)
    var token: String?,

    @OneToOne(targetEntity = User::class, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)

    var user: User,

    var expirationDate: Date?
) : EntityWithId()