package com.rready.copypaste

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class CopyPasteApplication

fun main(args: Array<String>) {
    runApplication<CopyPasteApplication>(*args)
}
