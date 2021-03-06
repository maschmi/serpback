package de.inw.serpent.serpback.user

import de.inw.serpent.serpback.SerpbackApplication
import de.inw.serpent.serpback.container.AbstractContainerDatabaseTest
import de.inw.serpent.serpback.user.controller.UserControllerError
import de.inw.serpent.serpback.user.domain.User
import de.inw.serpent.serpback.user.domain.UserAuthorities
import de.inw.serpent.serpback.user.service.UserServiceError
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.*
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.*
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.lang.AssertionError
import java.util.*
import javax.servlet.http.Cookie
import javax.transaction.Transactional
import kotlin.collections.HashSet

@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@SpringBootTest(classes = [SerpbackApplication::class])
class UserUATests : AbstractContainerDatabaseTest() {

    @Autowired
    private lateinit var sut: MockMvc

    @Autowired
    private lateinit var tokenStore: TestTokenStore

    @Autowired
    private lateinit var userRepo: UserRepository

    @Autowired
    private lateinit var userAuthoritiesRepo: UserAuthoritiesRepository

    @Autowired
    private lateinit var passwordEncode: PasswordEncoder

    private var token: String = ""

    @Test
    @Order(1)
    fun getEchoService_WhenNotAuthenticated_Returns403() {
        val expectedBody = UUID.randomUUID().toString()
        sut.perform(get("/echo/$expectedBody"))
            .andExpect(status().isForbidden)
    }

    @Test
    @Order(2)
    fun registerNewUser_WithEmptyObject_Returns400AndAllInvalidFields() {
        val payload = "{\n" +
                "            \"password\":  \"\",\n" +
                "            \"email\":  \"\",\n" +
                "            \"login\": \"\"\n" +
                "        }"
        performRegistrationRequest(payload)
            .andExpect {
                status { isBadRequest }
                content { contentType(MediaType.APPLICATION_JSON)}
                jsonPath("$.invalidfields") { isArray }
                jsonPath("$.invalidfields", hasItem("password"))
                jsonPath("$.invalidfields", hasItem("email"))
                jsonPath("$.invalidfields", hasItem("login"))
            }
        assertThat(tokenStore.registrationStore).isEmpty()
    }


    @Test
    @Order(3)
    fun registerNewUser_WithValidData_ReturnsUserDtoAndSendsEventWithRegistrationToken() {
        val payload = "{\n" +
                "            \"password\":  \"mytopsecret\",\n" +
                "            \"email\":  \"test@test\",\n" +
                "            \"login\": \"test\"\n" +
                "        }"
        val expectedResult = "{\n" +
                "            \"email\":  \"test@test\",\n" +
                "            \"login\": \"test\"\n" +
                "        }"

        performRegistrationRequest(payload)
            .andExpect {
                status { isCreated }
                content { contentType(MediaType.APPLICATION_JSON)}
                content { json(expectedResult) }
            }
        assertThat(tokenStore.registrationStore).isNotEmpty
        assertThat(tokenStore.registrationStore[1]).isNotEmpty
    }

    private fun performRegistrationRequest(payload: String) = sut.post("/api/user/register") {
        contentType = MediaType.APPLICATION_JSON
        content = payload
    }

    @Test
    @Order(4)
    fun activatesNewUser_UnknownToken_ReturnsOK() {
        val token = UUID.randomUUID().toString()

        sut.get("/api/user/register/$token")
            .andExpect {
                status { isBadRequest }
                contains(UserServiceError.USER_ALREADY_CONFIRMED.toString())
            }

    }

    @Test
    @Order(5)
    fun loginNewUser_UserNotActivated_ReturnsBadRequest() {
        val payload = getValidLoginPayload()

        performLogin(payload)
            .andExpect {
                status { isBadRequest }
                contains(UserControllerError.USER_NOT_ACTIVATED.toString())
            }
    }

    @Test
    @Order(6)
    fun activatesNewUser_CorrectToken_ReturnsOK() {
        val uid = getUidForLogin("test")
        val token = tokenStore.registrationStore[uid] ?: throw AssertionError("Token for uid $uid not found")

        sut.get("/api/user/register/$token")
            .andExpect {
                status { isOk }
            }

    }

    private fun getUidForLogin(login: String) = userRepo.findByLogin(login)?.id ?: throw AssertionError("User not found")

    @Test
    @Order(7)
    fun loginNewUser_UserIsActivated_ReturnsUserLoginResponse() {
        val payload = getValidLoginPayload()
        performLogin(payload)
            .andExpect {
                status { isOk  }
                content { contentType(MediaType.APPLICATION_JSON)}
                jsonPath("$.login", `is`("test") )
            }
    }

    private fun performLogin(payload: String) = sut.post("/api/user/login") {
        contentType = MediaType.APPLICATION_JSON
        content = payload
    }

    private fun getValidLoginPayload() = "{\n" +
            "  \"username\": \"test\",\n" +
            "  \"password\": \"mytopsecret\"\n" +
            "}"

    @Test
    @Order(8)
    fun getEchoService_WhenAuthenticated_ReturnsString() {
        val cookies = extractCookiesFromLoginResponse()
        val expectedBody = UUID.randomUUID().toString()
        sut.get("/api/echo/$expectedBody") {
            cookie( *cookies )
        }
            .andExpect {
                status { isOk }
                contains(expectedBody)
           }
    }

    private fun extractCookiesFromLoginResponse(payload: String? = null): Array<out Cookie> {
        val loginResult = performLogin(payload ?: getValidLoginPayload()).andReturn()
        return  loginResult.response.cookies
    }

    @Test
    @Order(9)
    fun getAdminEchoService_WhenUserIsNoAdmin_ReturnsForbidden() {
        val expectedBody = UUID.randomUUID().toString()
        val cookies = extractCookiesFromLoginResponse()
        sut.get("/api/echo/admin/$expectedBody") {
            cookie(*cookies)
        }
            .andExpect {
                status { isForbidden }
            }
    }

    @Test
    @Order(10)
    fun deleteUser_WhenUserIsNoAdmin_ReturnsForbidden() {
        val expectedBody = UUID.randomUUID().toString()
        val cookies = extractCookiesFromLoginResponse()
        sut.delete("/api/user/delete/$expectedBody") {
            cookie(*cookies)
        }
            .andExpect {
                status { isForbidden }
            }
    }

    @Test
    @Order(11)
    fun getAdminEchoService_WhenUserIsAdmin_ReturnsEcho() {
        addUserToAdminGroup("test")
        val someUserName = UUID.randomUUID().toString()
        val cookies = extractCookiesFromLoginResponse()
        sut.get("/api/echo/admin/$someUserName"){
            cookie(*cookies)
        }
            .andExpect {
                status { isOk }
                contains(someUserName)
            }
    }

    @Transactional
    protected fun addUserToAdminGroup(login: String) {
        val user = userRepo.findByLogin(login) ?: throw AssertionError("User $login not found!")
        val users = HashSet<User>()
        users.add(user)
        val auth = UserAuthorities("ROLE_ADMIN", users)
        userAuthoritiesRepo.save(auth)
    }

    @Test
    @Transactional
    @Order(12)
    fun deleteUser_WhenUserIsAdmin_ReturnsNoContentAndDeletesUser() {
        val userLogin = "deltest"
        val newUserToRegister = "{\n" +
                "            \"password\":  \"mytopsecret\",\n" +
                "            \"email\":  \"test1@test1\",\n" +
                "            \"login\": \"$userLogin\"\n" +
                "        }"
        performRegistrationRequest(newUserToRegister)
        addUserToAdminGroup(userLogin)
        assertThat(userRepo.findByLogin(userLogin)).isNotNull

        val cookies = extractCookiesFromLoginResponse()
        sut.delete("/api/user/delete/$userLogin") {
            cookie(*cookies)
        }
            .andExpect {
                status { isNoContent }
            }

        assertThat(userRepo.findByLogin(userLogin)).isNull()
        assertThat(userRepo.findByLogin("test")?.authorities?.map { u -> u.authority }).contains("ROLE_ADMIN")
    }

    @Test
    @Order(13)
            fun userLogout_userLogsOut_ReturnsOkAndEchoServiceIsNoLongerAccessible() {
        val expectedBody = UUID.randomUUID().toString()

        sut.get("/api/user/logout")
            .andExpect {
                status { isOk }
            }


        sut.get("/echo/admin/$expectedBody")
            .andExpect {
                status { isForbidden }
            }
    }

    @Test
    @Order(14)
    fun userLogin_userLogsInWithEmail_Succeeds() {
        val payload = getValidEmailLoginPayload()
        performLogin(payload)
            .andExpect {
                status { isOk  }
                content { contentType(MediaType.APPLICATION_JSON)}
                jsonPath("$.login", `is`("test") )
            }
    }

    @Test
    @Order(15)
    fun userLogin_adminUserLogsInAndHasRole_ReturnsUsernameAndRoleList() {
        val payload = getValidEmailLoginPayload()
        performLogin(payload)
            .andExpect {
                status { isOk  }
                content { contentType(MediaType.APPLICATION_JSON)}
                jsonPath("$.login", `is`("test") )
                jsonPath("$.authorities") { isArray }
                jsonPath("$.authorities", hasItem("ROLE_ADMIN"))
            }
    }

    @Test
    @Order(15)
    fun userDetails_() {
        val payload = getValidEmailLoginPayload()
        performLogin(payload)
            .andExpect {
                status { isOk  }
                content { contentType(MediaType.APPLICATION_JSON)}
                jsonPath("$.login", `is`("test") )
                jsonPath("$.authorities") { isArray }
                jsonPath("$.authorities", hasItem("ROLE_ADMIN"))
            }
    }

    private fun getValidEmailLoginPayload() = "{\n" +
            "  \"username\": \"test@test\",\n" +
            "  \"password\": \"mytopsecret\"\n" +
            "}"

    @Test
    @Order(20)
    fun resetPassword_StartPasswordResetWithUnknownUser_ReturnsOK() {
        val payload = "{ \"login\": \"deltest\"}"
        sut.post("/api/user/reset/init") {
            content = payload
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk }
        }
    }

    @Test
    @Order(21)
    fun resetPassword_StartPasswordResetWithKnownUser_PublishesEventAndReturnsOK() {
        val resetStoreSize = tokenStore.resetStore.size
        val payload = "{ \"login\": \"test\"}"
        sut.post("/api/user/reset/init") {
            content = payload
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk }
        }
        assertThat(tokenStore.resetStore.size).isEqualTo(resetStoreSize + 1)
    }

    @Test
    @Order(25)
    fun resetPassword_finishResetWithWrongToken_DoesNotUpdateAnyPasswordsAndReturnsOk() {
        val oldPasswords = getAllHashedPasswords()
        val payload = "{ \"password\": \"newsecret\"}"
        val token = UUID.randomUUID().toString()
        sut.post("/api/user/reset/finish/$token") {
            content = payload
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk }
        }
        val newPasswords = getAllHashedPasswords()
        assertThat(newPasswords).containsAll(oldPasswords)
        assertThat(newPasswords.size).isEqualTo(oldPasswords.size)
    }

    private fun getAllHashedPasswords() = userRepo.findAll().mapNotNull { u -> u.password }

    @Test
    @Order(26)
    fun resetPassword_finishResetWithCorrectToken_DoesUpdatePasswordAndReturnsOk() {
        val oldPasswords = getAllHashedPasswords()
        val uid = getUidForLogin("test")
        val oldUserPassword = userRepo.findById(uid).orElse(null)?.password ?: ""
        val unaffectedPasswords = oldPasswords.filter { pw -> pw != oldUserPassword }
        val newPassword = "newsecret"
        val token = tokenStore.resetStore[uid]
        performFinishPasswordReset(token, newPassword).andExpect {
            status { isOk }
        }
        val newPasswords = getAllHashedPasswords()
        val newUserPassword = userRepo.findById(uid).orElse(null)?.password ?: ""
        assertThat(newPasswords).containsAll(unaffectedPasswords)
        assertThat(newPasswords.size).isEqualTo(oldPasswords.size)
        assertThat(passwordEncode.matches("newsecret", newUserPassword)).isTrue
    }

    private fun performFinishPasswordReset(
        token: String?,
        password: String
    ) = sut.post("/api/user/reset/finish/$token") {
        content = "{ \"password\": \"$password\"}"
        contentType = MediaType.APPLICATION_JSON
    }

    @Test
    @Order(30)
    fun createUser_adminUser_CreatesUserWithKnownAndUnknownAuthority_SendsPasswordResetEventForNewUser_ReturnsUserWithoutUnknownAuthority() {
        val adminLoginPayload = adminLoginAfterPasswordChange()
        val cookies = extractCookiesFromLoginResponse(adminLoginPayload)
        val newUserPayload = "{\n" +
                "            \"email\":  \"test1@test1\",\n" +
                "            \"login\": \"test1\",\n" +
                "            \"authorities\": [\"ROLE_ADMIN\", \"ROLE_USER\"] "+
                "        }"
        val expectedResult = "{\n" +
                "            \"email\":  \"test1@test1\",\n" +
                "            \"login\": \"test1\",\n" +
                "            \"authorities\": [\"ROLE_ADMIN\"] "+
                "        }"
        sut.post("/api/user/create") {
            contentType = MediaType.APPLICATION_JSON
            content = newUserPayload
            cookie(*cookies)
        }
            .andExpect {
                status { isCreated }
                content { contentType(MediaType.APPLICATION_JSON)}
                content { json(expectedResult) }
            }

        val newUserUid = getUidForLogin("test1")

        val resetToken = tokenStore.resetStore[newUserUid]
        assertThat(resetToken).isNotNull

        performFinishPasswordReset(resetToken, "test1")
            .andExpect { status { isOk } }
    }

    @Test
    @Order(31)
    fun createUser_adminUser_CreatesUserExistingEmail_GetsError() {
        val adminLoginPayload = adminLoginAfterPasswordChange()
        val cookies = extractCookiesFromLoginResponse(adminLoginPayload)
        val newUserPayload = "{\n" +
                "            \"email\":  \"test1@test1\",\n" +
                "            \"login\": \"test2\",\n" +
                "            \"authorities\": [\"ROLE_ADMIN\", \"ROLE_USER\"] "+
                "        }"
        sut.post("/api/user/create") {
            contentType = MediaType.APPLICATION_JSON
            content = newUserPayload
            cookie(*cookies)
        }
            .andExpect {
                status { isBadRequest }
                contains(UserServiceError.EMAIL_ALREADY_REGISTERED.toString())
            }
    }

    @Test
    @Order(32)
    fun createUser_adminUser_CreatesUserExistingLogin_GetsError() {
        val adminLoginPayload = adminLoginAfterPasswordChange()
        val cookies = extractCookiesFromLoginResponse(adminLoginPayload)
        val newUserPayload = "{\n" +
                "            \"email\":  \"test2@test1\",\n" +
                "            \"login\": \"test1\",\n" +
                "            \"authorities\": [\"ROLE_ADMIN\", \"ROLE_USER\"] "+
                "        }"
        sut.post("/api/user/create") {
            contentType = MediaType.APPLICATION_JSON
            content = newUserPayload
            cookie(*cookies)
        }
            .andExpect {
                status { isBadRequest }
                contains(UserServiceError.LOGIN_ALREADY_REGISTERED.toString())
            }
    }

    private fun adminLoginAfterPasswordChange() = "{\n" +
            "  \"username\": \"test@test\",\n" +
            "  \"password\": \"newsecret\"\n" +
            "}"

    @Test
    @Order(40)
    fun changeAuthorities_adminUser_ReturnsUserDetailsWithNewAuthorities() {
        val adminLoginPayload = adminLoginAfterPasswordChange()
        val cookies = extractCookiesFromLoginResponse(adminLoginPayload)
        val newAuthorities = "{ \"authorities\": [] }"
        val userToUpdate = "test1"
        val expectedResult = "{\n" +
                "            \"email\":  \"test1@test1\",\n" +
                "            \"login\": \"test1\",\n" +
                "            \"authorities\": [] "+
                "        }"
        sut.get("/api/user/$userToUpdate") {
            cookie(*cookies)
        }
            .andExpect {
                status { isOk }
                jsonPath("$.authorities", hasItem("ROLE_ADMIN"))

            }

        performAuthoritiesUpdate(userToUpdate, newAuthorities, cookies).andExpect {
            status { isOk }
            content { contentType(MediaType.APPLICATION_JSON) }
            content { json(expectedResult) }
        }

        sut.get("/api/user/$userToUpdate") {
            cookie(*cookies)
        }
            .andExpect {
                status { isOk }
                content { json(expectedResult) }
            }
    }

    private fun performAuthoritiesUpdate(
        userToUpdate: String,
        newAuthorities: String,
        cookies: Array<out Cookie>
    ) = sut.put("/api/user/$userToUpdate/authorities") {
        contentType = MediaType.APPLICATION_JSON
        content = newAuthorities
        cookie(*cookies)
    }

    @Test
    @Order(41)
    fun changeAuthorities_nonAdminUser_ReturnsForbidden() {
        val cookies = extractCookiesFromLoginResponse(nonAdminLoginPayload())
        val newAuthorities = "{ \"authorities\": [\"ROLE_ADMIN\"] }"
        val userToUpdate = "test1"
        performAuthoritiesUpdate(userToUpdate, newAuthorities, cookies)
            .andExpect { status { isForbidden } }
    }

    private fun nonAdminLoginPayload() = "{\n" +
            "  \"username\": \"test1\",\n" +
            "  \"password\": \"test1\"\n" +
            "}"

    @Test
    @Order(50)
    fun getUser_adminUser_returnsAllUsers() {
        val adminLoginPayload = adminLoginAfterPasswordChange()
        val cookies = extractCookiesFromLoginResponse(adminLoginPayload)
        val expectedResult = "[" +
                "{\"login\":\"test\",\"email\":\"test@test\",\"authorities\":[\"ROLE_ADMIN\"]}," +
                "{\"login\":\"test1\",\"email\":\"test1@test1\",\"authorities\":[]}" +
                "]"
        sut.get("/api/user") {
            cookie(*cookies)
        }.andExpect {
            status { isOk }
            content { contentType(MediaType.APPLICATION_JSON) }
            content { json(expectedResult) }
        }
    }

    @Test
    @Order(51)
    fun getUser_nonAdminUser_returnsUnauthorized() {
        val loginPayload = nonAdminLoginPayload()
        val cookies = extractCookiesFromLoginResponse(loginPayload)
        sut.get("/api/user") {
            cookie(*cookies)
        }.andExpect {
            status { isForbidden }
        }
    }

    @Test
    @Order(60)
    fun getSingleUser_nonAdminUserButSelf_returnsDetails() {
        val loginPayload = nonAdminLoginPayload()
        val cookies = extractCookiesFromLoginResponse(loginPayload)
        val expectedResult = "{\n" +
                "            \"email\":  \"test1@test1\",\n" +
                "            \"login\": \"test1\",\n" +
                "            \"authorities\": [] "+
                "        }"
        sut.get("/api/user/test1") {
            cookie(*cookies)
        }.andExpect {
            status { isOk }
            content { contentType(MediaType.APPLICATION_JSON) }
            content { json(expectedResult) }
        }
    }

    @Test
    @Order(61)
    fun getSingleUser_nonAdminUserAndNotSelf_returnsForbidden() {
        val loginPayload = nonAdminLoginPayload()
        val cookies = extractCookiesFromLoginResponse(loginPayload)
        sut.get("/api/user/test") {
            cookie(*cookies)
        }.andExpect {
            status { isUnauthorized }
        }
    }

    @Test
    @Order(62)
    fun getSingleUser_adminUserAndNotSelf_returnsDetails() {
        val loginPayload = adminLoginAfterPasswordChange()
        val cookies = extractCookiesFromLoginResponse(loginPayload)
        val expectedResult = "{\n" +
                "            \"email\":  \"test1@test1\",\n" +
                "            \"login\": \"test1\",\n" +
                "            \"authorities\": [] "+
                "        }"
        sut.get("/api/user/test1") {
            cookie(*cookies)
        }.andExpect {
            status { isOk }
            content { contentType(MediaType.APPLICATION_JSON) }
            content { json(expectedResult) }
        }
    }


    @Test
    @Order(70)
    fun updateUser_adminUserAndNotSelf_returnsDetailsWithUpdatedAuthorities() {
        val loginPayload = adminLoginAfterPasswordChange()
        val cookies = extractCookiesFromLoginResponse(loginPayload)
        val expectedResult = "{\n" +
                "            \"email\":  \"test1@test1\",\n" +
                "            \"login\": \"test1\",\n" +
                "            \"authorities\": [\"ROLE_ADMIN\"] "+
                "        }"
        val userToUpdate = "test1"
        performUpdateUserPut(userToUpdate, expectedResult, cookies)
            .andExpect {
                status { isOk }
                content { contentType(MediaType.APPLICATION_JSON) }
                content { json(expectedResult) }
            }

        performAuthoritiesUpdate(userToUpdate, "{ \"authorities\": [] }", cookies)
    }

    private fun performUpdateUserPut(
        userToUpdate: String,
        expectedResult: String,
        cookies: Array<out Cookie>
    ): ResultActionsDsl {
        return sut.put("/api/user/$userToUpdate") {
            contentType = MediaType.APPLICATION_JSON
            content = expectedResult
            cookie(*cookies)
        }
    }

    @Test
    @Order(71)
    fun updateUser_adminUserAndSelfUpdatesToExistingLogin_ReturnsError() {
            val loginPayload = adminLoginAfterPasswordChange()
            val cookies = extractCookiesFromLoginResponse(loginPayload)
            val expectedResult = "{\n" +
                    "            \"email\":  \"test1@test1\",\n" +
                    "            \"login\": \"test\",\n" +
                    "            \"authorities\": [\"ROLE_ADMIN\"] "+
                    "        }"
            val userToUpdate = "test1"

            performUpdateUserPut(userToUpdate, expectedResult, cookies)
                .andExpect {
                    status { isBadRequest }
                    contains(UserServiceError.LOGIN_ALREADY_REGISTERED.toString())
                }
    }

    @Test
    @Order(72)
    fun updateUser_adminUserAndSelfUpdatesToExistingEmail_ReturnsError() {
        val loginPayload = adminLoginAfterPasswordChange()
        val cookies = extractCookiesFromLoginResponse(loginPayload)
        val expectedResult = "{\n" +
                "            \"email\":  \"test@test\",\n" +
                "            \"login\": \"test1\",\n" +
                "            \"authorities\": [\"ROLE_ADMIN\"] "+
                "        }"
        val userToUpdate = "test1"

        performUpdateUserPut(userToUpdate, expectedResult, cookies)
            .andExpect {
                status { isBadRequest }
                contains(UserServiceError.EMAIL_ALREADY_REGISTERED.toString())
            }
    }

    @Test
    @Order(73)
    fun updateUser_nonAdminUserAndNotSelf_returnsUnauthorized() {
        val loginPayload = nonAdminLoginPayload()
        val cookies = extractCookiesFromLoginResponse(loginPayload)
        val expectedResult = "{\n" +
                "            \"email\":  \"test@test\",\n" +
                "            \"login\": \"test\",\n" +
                "            \"authorities\": [] "+
                "        }"
        val userToUpdate = "test"

        performUpdateUserPut(userToUpdate, expectedResult, cookies)
            .andExpect {
                status { isUnauthorized }
            }
    }

    @Test
    @Order(74)
    fun updateUser_nonAdminUserAndSelf_returnsNewDetailsWithUnchangedAuthorities() {
        val loginPayload = nonAdminLoginPayload()
        val cookies = extractCookiesFromLoginResponse(loginPayload)
        val updatePayload= "{\n" +
                "            \"email\":  \"test1@test1\",\n" +
                "            \"login\": \"test3\",\n" +
                "            \"authorities\": [\"ROLE_ADMIN\"] "+
                "        }"
        val expectedResult = "{\n" +
                "            \"email\":  \"test1@test1\",\n" +
                "            \"login\": \"test3\",\n" +
                "            \"authorities\": [] "+
                "        }"
        val userToUpdate = "test1"

        performUpdateUserPut(userToUpdate, updatePayload, cookies)
            .andExpect {
                status { isOk }
                content { contentType(MediaType.APPLICATION_JSON) }
                content { json(expectedResult) }
            }
    }

}