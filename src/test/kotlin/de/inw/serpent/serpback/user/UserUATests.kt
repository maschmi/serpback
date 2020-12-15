package de.inw.serpent.serpback.user

import de.inw.serpent.serpback.SerpbackApplication
import de.inw.serpent.serpback.container.AbstractContainerDatabaseTest
import de.inw.serpent.serpback.user.controller.UserControllerError
import de.inw.serpent.serpback.user.domain.User
import de.inw.serpent.serpback.user.domain.UserAuthorities
import de.inw.serpent.serpback.user.events.UserRegisteredEvent
import de.inw.serpent.serpback.user.service.UserServiceError
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.event.EventListener
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
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
                "            \"firstName\":  \"\",\n" +
                "            \"lastName\":  \"\",\n" +
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
        assertThat(tokenStore.store).isEmpty()
    }


    @Test
    @Order(3)
    fun registerNewUser_WithValidData_ReturnsUserDtoAndSendsEventWithRegistrationToken() {
        val payload = "{\n" +
                "            \"firstName\":  \"fn\",\n" +
                "            \"lastName\":  \"ln\",\n" +
                "            \"password\":  \"mytopsecret\",\n" +
                "            \"email\":  \"test@test\",\n" +
                "            \"login\": \"test\"\n" +
                "        }"
        val expectedResult = "{\n" +
                "            \"firstName\":  \"fn\",\n" +
                "            \"lastName\":  \"ln\",\n" +
                "            \"email\":  \"test@test\",\n" +
                "            \"login\": \"test\"\n" +
                "        }"

        performRegistrationRequest(payload)
            .andExpect {
                status { isCreated }
                content { contentType(MediaType.APPLICATION_JSON)}
                content { json(expectedResult) }
            }
        assertThat(tokenStore.store).isNotEmpty
        assertThat(tokenStore.store[1]).isNotEmpty
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
        val token = tokenStore.store[uid] ?: throw AssertionError("Token for uid $uid not found")

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

    private fun extractCookiesFromLoginResponse(): Array<out Cookie> {
        val loginResult = performLogin(getValidLoginPayload()).andReturn()
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
                "            \"firstName\":  \"fn\",\n" +
                "            \"lastName\":  \"ln\",\n" +
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
    fun userLogout_userLogsOut_ReturnsOk_AndEchoServiceIsNoLongerAccessible() {
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
    @Order(13)
    fun userLogin_userLogsInWithEmail_Succeeds() {
        val payload = getValidEmailLoginPayload()
        performLogin(payload)
            .andExpect {
                status { isOk  }
                content { contentType(MediaType.APPLICATION_JSON)}
                jsonPath("$.login", `is`("test") )
            }
    }

    private fun getValidEmailLoginPayload() = "{\n" +
            "  \"username\": \"test@test\",\n" +
            "  \"password\": \"mytopsecret\"\n" +
            "}"


    @EventListener
    fun userRegistrationListener(event: UserRegisteredEvent) {
        this.token = event.token
    }

}