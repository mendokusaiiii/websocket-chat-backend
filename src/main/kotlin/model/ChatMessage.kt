package org.mendo.model

data class ChatMessage(
    val type: MessageType,
    val content: String,
    val sender: String
)

enum class MessageType {
    CHAT,
    JOIN,
    LEAVE
}