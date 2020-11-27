package de.inw.serpent.serpback.user.controller.exception

import de.inw.serpent.serpback.user.service.UserServiceError

open class UserServiceException(val reason: UserServiceError) : Exception() {

}

