package de.inw.serpent.serpback.user.events

import org.springframework.context.ApplicationEvent

class PasswordResetEvent(source: Any, val id: Long?, val token: String) : ApplicationEvent(source)
