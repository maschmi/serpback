package de.inw.serpent.serpback.user.controller.exception

import de.inw.serpent.serpback.user.service.UserServiceError

class UserUpdateException(reason: UserServiceError) : UserServiceException(reason)
