package de.inw.serpent.serpback.domain

import org.springframework.data.util.ProxyUtils
import javax.persistence.*

@MappedSuperclass
abstract class EntityWithId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SequenceGenerator(name = "id_generator", sequenceName = "id_seq", allocationSize = 1)
    var id: Long? = null

    override fun equals(other: Any?): Boolean {
        other ?: return false

        if (this === other) return true

        if (javaClass != ProxyUtils.getUserClass(other)) return false

        other as EntityWithId

        return this.id != null && this.id == other.id
    }

    override fun hashCode() = 37

    override fun toString(): String {
        return "${this.javaClass.simpleName}(id=$id)"
    }

}
