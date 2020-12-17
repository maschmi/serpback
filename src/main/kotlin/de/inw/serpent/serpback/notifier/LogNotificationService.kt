package de.inw.serpent.serpback.notifier

import de.inw.serpent.serpback.user.events.PasswordResetEvent
import de.inw.serpent.serpback.user.events.UserRegisteredEvent
import de.inw.serpent.serpback.user.service.PasswordResetNotifier
import de.inw.serpent.serpback.user.service.RegistrationNotifier
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class LogNotificationService : RegistrationNotifier, PasswordResetNotifier {

    private val log = LoggerFactory.getLogger(LogNotificationService::class.java)

    override fun onRegistration(event: UserRegisteredEvent) {
        log.info("User registered with id: {} and token {}", event.id, event.token)
    }

    override fun onPasswordResetStart(event: PasswordResetEvent) {
        log.info("User password reset initiated: uid: {}, token {}", event.id, event.token)
    }
}