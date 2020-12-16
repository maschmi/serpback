package de.inw.serpent.serpback.user

import de.inw.serpent.serpback.user.events.PasswordResetEvent
import de.inw.serpent.serpback.user.service.RegistrationNotifier


import de.inw.serpent.serpback.user.events.UserRegisteredEvent
import de.inw.serpent.serpback.user.service.PasswordResetNotifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty("featuretoggle.test")
class TestTokenStore : RegistrationNotifier, PasswordResetNotifier {

    val registrationStore = HashMap<Long, String>()
    val resetStore = HashMap<Long, String>()

    override fun onRegistration(event: UserRegisteredEvent) {
        registrationStore[event.id ?: -1] = event.token
    }

    override fun onPasswordResetStart(event: PasswordResetEvent) {
        resetStore[event.id ?: -1] = event.token
    }
}