package de.inw.serpent.serpback.user.service

import de.inw.serpent.serpback.user.events.PasswordResetEvent
import org.springframework.context.event.EventListener

interface PasswordResetNotifier {

    @EventListener
    fun onPasswordResetStart(event: PasswordResetEvent)
}