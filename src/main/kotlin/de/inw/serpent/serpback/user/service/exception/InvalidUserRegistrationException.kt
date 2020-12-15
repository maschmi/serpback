package de.inw.serpent.serpback.user.service.exception

import de.inw.serpent.serpback.user.service.UserServiceError
import java.lang.Exception

class InvalidUserRegistrationException(val invalidFields: List<String>) : Exception() {

}
