package de.inw.serpent.serpback.user.events

import de.inw.serpent.serpback.user.service.UserService
import org.springframework.context.ApplicationEvent

class UserRegisteredEvent(source: Any, val id: Long?, val token: String) : ApplicationEvent(source) {

}
