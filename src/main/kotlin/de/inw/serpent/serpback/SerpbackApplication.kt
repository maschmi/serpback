package de.inw.serpent.serpback

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SerpbackApplication

fun main(args: Array<String>) {
    runApplication<SerpbackApplication>(*args)
}
