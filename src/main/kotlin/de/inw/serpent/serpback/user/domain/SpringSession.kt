package de.inw.serpent.serpback.user.domain

import org.springframework.web.bind.annotation.SessionAttribute
import java.io.Serializable
import javax.persistence.*

@Entity
@Table(name = "SPRING_SESSION",
indexes = [
    Index(name = "idx_session_sid", columnList = "SESSION_ID"),
    Index(name = "idx_session_expiry", columnList = "EXPIRY_TIME")])
class SpringSession(

    @Id
    @Column(name ="PRIMARY_ID", length = 36, nullable =  false)
    val primaryId: String,

    @Column(name ="SESSION_ID", length = 36, nullable = false)
    val sessionId: String,

    @Column(name ="CREATION_TIME", nullable = false)
    val creationTime: Long,

    @Column(name ="LAST_ACCESS_TIME", nullable = false)
    val lastAccessTime: Long,

    @Column(name ="MAX_INACTIVE_INTERVAL", nullable = false)
    val maxInactiveInterval: Int,

    @Column(name ="EXPIRY_TIME", nullable = false)
    val expiryTime: Long,

    @Column(name ="PRINCIPAL_NAME", length = 100)
    val principalName: String,

    @OneToMany(mappedBy = "sessionPrimaryId", cascade = [CascadeType.REMOVE])
    val sessionAttribute: List<SpringSessionAttribute>

    ) : Serializable
