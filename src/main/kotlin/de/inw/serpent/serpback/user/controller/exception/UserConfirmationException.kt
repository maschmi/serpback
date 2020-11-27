package de.inw.serpent.serpback.user.controller.exception

import de.inw.serpent.serpback.user.service.UserServiceError

class UserConfirmationException(reason: UserServiceError) : UserServiceException(reason) {

}
