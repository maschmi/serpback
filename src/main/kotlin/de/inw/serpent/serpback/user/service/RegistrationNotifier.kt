package de.inw.serpent.serpback.user.service

import de.inw.serpent.serpback.user.events.UserRegisteredEvent
import org.springframework.context.event.EventListener

interface RegistrationNotifier {

    @EventListener()
    fun onRegistration(event: UserRegisteredEvent)

}

