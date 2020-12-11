package de.inw.serpent.serpback.echo

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.security.access.annotation.Secured
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@ConditionalOnProperty("featuretoggle.test")
@RestController
@RequestMapping("/api")
class EchoController {

    @GetMapping("/echo/{stringtoecho}")
    fun echo(@PathVariable("stringtoecho") echo: String?) : ResponseEntity<String> {
        if (echo.isNullOrBlank()) {
            return ResponseEntity.badRequest().build()
        }
        return ResponseEntity.ok(echo)
    }

    @GetMapping("/echo/admin/{stringtoecho}")
    @Secured("ROLE_ADMIN")
    fun echoAdmin(@PathVariable("stringtoecho") echo: String?) : ResponseEntity<String> {
        if (echo.isNullOrBlank()) {
            return ResponseEntity.badRequest().build()
        }
        return ResponseEntity.ok(echo)
    }
}