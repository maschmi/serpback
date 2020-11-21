package de.inw.serpent.serpback.user.controller.exception

import java.lang.Exception

class InvalidUserRegistrationException(val invalidFields: String) : Exception() {

}
