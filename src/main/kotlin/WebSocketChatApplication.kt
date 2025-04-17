package org.mendo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class WebSocketChatApplication
fun main(args: Array<String>) {
    runApplication<WebSocketChatApplication>(*args)
}