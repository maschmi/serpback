package de.inw.serpent.serpback.user.domain

import org.hibernate.annotations.Type
import java.io.Serializable
import javax.persistence.*

@Entity
@Table(name = "SPRING_SESSION_ATTRIBUTES")
@IdClass(SessionAttributeKey::class)
class SpringSessionAttribute(

    @Id
    @JoinColumn(name = "SESSION_PRIMARY_ID", nullable = false)
    @ManyToOne
    val sessionPrimaryId: SpringSession,

    @Id
    @Column(name = "ATTRIBUTE_NAME", nullable = false, length = 200)
    val attributeName: String,

    @Lob
    @Type(type = "org.hibernate.type.BinaryType")
    @Column(name = "ATTRIBUTE_BYTES", nullable = false)
    val attributeBytes: Array<Byte>

) : Serializable