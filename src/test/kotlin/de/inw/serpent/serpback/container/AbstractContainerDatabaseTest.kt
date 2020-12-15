package de.inw.serpent.serpback.container

import org.junit.platform.commons.logging.LoggerFactory
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
abstract class AbstractContainerDatabaseTest {


    companion object {
        private val log = LoggerFactory.getLogger(AbstractContainerDatabaseTest::class.java)

        @Container
        val container = PostgreSQLContainer<Nothing>("postgres:13")
            .apply {
                withDatabaseName("serp")
                withUsername("testuser")
                withPassword("secret")
            }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            val jdbcUrl = container::getJdbcUrl
            registry.add("spring.datasource.url", jdbcUrl)
            registry.add("spring.datasource.password", container::getPassword)
            registry.add("spring.datasource.username", container::getUsername)
            registry.add("spring.liquibase.url", jdbcUrl)
            log.info { "Connecting to $jdbcUrl" }
        }

    }

}