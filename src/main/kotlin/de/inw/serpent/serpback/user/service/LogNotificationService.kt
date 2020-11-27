package de.inw.serpent.serpback.user.service

import de.inw.serpent.serpback.user.events.UserRegisteredEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class LogNotificationService : IRegistrationNotifier {

    private val log = LoggerFactory.getLogger(LogNotificationService::class.java)

    override fun onRegistration(event: UserRegisteredEvent) {
        log.info("User registered with id: {} and token {}", event.id, event.token)
    }
}