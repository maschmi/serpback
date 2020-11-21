package de.inw.serpent.serpback.user.controller.exception

import de.inw.serpent.serpback.user.service.UserServiceError

class UserRegistrationException(val reason: UserServiceError) : Exception() {

}
