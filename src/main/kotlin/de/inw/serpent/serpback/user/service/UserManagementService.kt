package de.inw.serpent.serpback.user.service

import de.inw.serpent.serpback.user.dto.UserDto
import org.springframework.stereotype.Service

@Service
class UserManagementService {

    fun startResetPassword(username: String) {
        // TODO: 15/12/2020
    }

    fun finishResetPassword(token: String, password: String) {
        // TODO: 15/12/2020
    }

    fun deleteUser(username: String) {

    }

    fun createUser(user: UserDto): UserDto {
        // todo create user
        startResetPassword(user.login)
        return user
    }
}
