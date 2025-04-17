package org.mendo.controller

import org.mendo.model.ChatMessage
import org.mendo.model.MessageType
import org.mendo.service.ChatUserService
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.stereotype.Controller

@Controller
class ChatController(
    private val chatUserService: ChatUserService,
    private val messagingTemplate: SimpMessageSendingOperations
) {

    private val logger = LoggerFactory.getLogger(ChatController::class.java)

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    fun sendMessage(@Payload chatMessage: ChatMessage): ChatMessage {
        if (chatMessage.type == MessageType.CHAT) {
            logger.info("Received CHAT message: from=${chatMessage.sender}, content='${chatMessage.content}'")
            return chatMessage
        } else {
            logger.warn("Received non-CHAT message type (${chatMessage.type}) on /chat.sendMessage endpoint from ${chatMessage.sender}")
            throw IllegalArgumentException("Invalid message type received on chat endpoint: ${chatMessage.type}")
        }
    }

    @MessageMapping("/chat.addUser")
    fun addUser(@Payload chatMessage: ChatMessage, headerAccessor: SimpMessageHeaderAccessor) {
        val username = chatMessage.sender
        val sessionId = headerAccessor.sessionId

        logger.info("User '$username' attempting to join with session ID: $sessionId")

        if (chatUserService.isUserOnline(username)) {
            logger.warn("User '$username' attempted to join but is already online. Session ID: $sessionId")
            return
        }

        headerAccessor.sessionAttributes?.put("username", username)
        logger.info("User '$username' session attributes set. Session ID: $sessionId")

        val added = chatUserService.addUser(username)

        if (added) {
            logger.info("User '$username' added successfully to online list.")
            val joinMessage = chatMessage.copy(type = MessageType.JOIN)
            messagingTemplate.convertAndSend("/topic/public", joinMessage)
            logger.info("Sent JOIN message for '$username' to /topic/public")

            val onlineUsers = chatUserService.getOnlineUsers()
            messagingTemplate.convertAndSend("/topic/users", onlineUsers)
            logger.info("Sent updated user list to /topic/users. Count: ${onlineUsers.size}")

        } else {
            logger.error("CRITICAL: Failed to add user '$username' even after isUserOnline check returned false. Session ID: $sessionId")
        }
    }
}