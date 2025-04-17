package org.mendo.listener

import org.mendo.model.ChatMessage
import org.mendo.model.MessageType
import org.mendo.service.ChatUserService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionDisconnectEvent

@Component
class WebSocketEventListener(
    private val messagingTemplate: SimpMessageSendingOperations,
    private val chatUserService: ChatUserService
) {
    private val logger = LoggerFactory.getLogger(WebSocketEventListener::class.java)

    @EventListener
    fun handleWebSocketDisconnectListener(event: SessionDisconnectEvent) {
        val headerAccessor = StompHeaderAccessor.wrap(event.message)
        val username = headerAccessor.sessionAttributes?.get("username") as String?

        if (username != null) {
            logger.info("User Disconnected: $username")

            val chatMessage = ChatMessage(
                type = MessageType.LEAVE,
                sender = username,
                content = "$username has left the chat"
            )

            messagingTemplate.convertAndSend("/topic/public", chatMessage)

            val onlineUsers = chatUserService.getOnlineUsers()
            messagingTemplate.convertAndSend("/topic/users", onlineUsers)

            logger.info("Sent LEAVE message and updated user list for $username. Online users: {${onlineUsers.size}}")
        } else {
            logger.warn("Could not find username for disconnected session: ${headerAccessor.sessionId}")
        }
    }
}