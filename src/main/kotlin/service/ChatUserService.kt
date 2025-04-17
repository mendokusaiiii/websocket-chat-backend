package org.mendo.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class ChatUserService {
    private val logger = LoggerFactory.getLogger(ChatUserService::class.java)

    private val onlineUsers: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun addUser(username: String): Boolean {
        val added = onlineUsers.add(username)

        if (added) {
            logger.info("User added to online users: $username . Current online users: ${onlineUsers.size}")
        } else {
            logger.warn("Attempt to add existing user to online list: $username")
        }
         return added
    }

    fun removeUser(username: String): Boolean {
        val removed = onlineUsers.remove(username)

        if (removed) {
            logger.info("User removed from online users: $username . Current online users: ${onlineUsers.size}")
        } else {
            logger.warn("Attempt to remove non-existing user from online list: $username")
        }
        return removed
    }

    fun getOnlineUsers(): Set<String> {
        return onlineUsers.toSet()
    }

    fun isUserOnline(username: String): Boolean {
        return onlineUsers.contains(username)
    }
}