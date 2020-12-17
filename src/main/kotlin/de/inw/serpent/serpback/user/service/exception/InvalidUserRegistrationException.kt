package de.inw.serpent.serpback.user.service.exception

class InvalidUserRegistrationException(val invalidFields: List<String>) : Exception()
