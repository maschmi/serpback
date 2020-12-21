package de.inw.serpent.serpback.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("application.cors")
data class CorsProperties(var allowedOrigins: List<String> = ArrayList()) {

}
