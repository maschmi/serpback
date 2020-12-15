package de.inw.serpent.serpback.user.service

import de.inw.serpent.serpback.user.UserRepository
import de.inw.serpent.serpback.user.dto.UserDto
import org.springframework.stereotype.Service
import javax.transaction.Transactional

@Service
@Transactional
class UserManagementService(private val userRepository: UserRepository) {

    fun startResetPassword(username: String) {
        // TODO: 15/12/2020
    }

    fun finishResetPassword(token: String, password: String) {
        // TODO: 15/12/2020
    }

    fun tryDeleteUser(username: String): Boolean {
        val uid = userRepository.findByLogin(username)?.id ?: return true
        userRepository.deleteById(uid)
        return true
    }

    fun createUser(user: UserDto): UserDto {
        // todo create user
        startResetPassword(user.login)
        return user
    }
}
