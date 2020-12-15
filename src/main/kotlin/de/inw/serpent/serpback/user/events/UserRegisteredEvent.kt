package de.inw.serpent.serpback.user.events

import org.springframework.context.ApplicationEvent

class UserRegisteredEvent(source: Any, val id: Long?, val token: String) : ApplicationEvent(source) {

}
