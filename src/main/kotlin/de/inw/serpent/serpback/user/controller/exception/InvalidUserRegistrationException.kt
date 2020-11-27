package de.inw.serpent.serpback.user.controller.exception

import de.inw.serpent.serpback.user.service.UserServiceError
import java.lang.Exception

class InvalidUserRegistrationException(val invalidFields: String) : Exception() {

}
