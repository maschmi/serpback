package de.inw.serpent.serpback.user.domain

import java.io.Serializable


class SessionAttributeKey(
    val sessionPrimaryId: SpringSession,
    val attributeName: String
) : Serializable {

}
