package de.inw.serpent.serpback.user

import de.inw.serpent.serpback.user.service.IRegistrationNotifier


import de.inw.serpent.serpback.user.events.UserRegisteredEvent
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty("featuretoggle.test")
class TestTokenStore : IRegistrationNotifier {

    val store = HashMap<Long, String>()

    override fun onRegistration(event: UserRegisteredEvent) {
        store[event.id ?: -1] = event.token
    }
}