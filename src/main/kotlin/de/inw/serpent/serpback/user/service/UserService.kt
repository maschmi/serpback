package de.inw.serpent.serpback.user.service

import de.inw.serpent.serpback.type.ErrorResult
import de.inw.serpent.serpback.user.UserRepository
import de.inw.serpent.serpback.user.domain.mapToDto
import de.inw.serpent.serpback.user.domain.mapToEntity
import de.inw.serpent.serpback.user.dto.AccountDto
import de.inw.serpent.serpback.user.dto.UserDto
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(private val userRepository: UserRepository,
                  private val passwordEncoder: PasswordEncoder) {
    fun registerUser(account: AccountDto): ErrorResult<UserDto, UserServiceError> {
        if (userRepository.findByLogin(account.login) != null) {
            return ErrorResult.failure(UserServiceError.LOGIN_ALREADY_REGISTERED)
        }

        if (userRepository.findByEmail(account.email) != null) {
            return ErrorResult.failure(UserServiceError.EMAIL_ALREADY_REGISTERED)
        }

        val result = userRepository
            .save(account.mapToEntity(passwordEncoder, true))
            .mapToDto()

        return ErrorResult.success(result)
    }
}



