# Backend Documentation Summary: Kotlin/Spring Boot WebSocket Chat

## 1. Introduction

This document summarizes the backend system for a real-time chat application built with Kotlin and Spring Boot, utilizing WebSockets for communication.

**1.1. Purpose:**
To provide a scalable and robust backend supporting real-time multi-user chat features, including public messaging, user presence notifications (join/leave), and an online user list.

**1.2. Key Technologies:**
* **Kotlin:** Primary language, offering conciseness and null-safety.
* **Spring Boot:** Core framework for rapid application development and configuration.
* **Spring WebSocket:** Module for WebSocket support.
* **STOMP:** Messaging protocol over WebSocket for simplified client-server communication via destinations.
* **SockJS:** Used as a fallback mechanism for clients lacking native WebSocket support.

**1.3. Implemented Features:**
* Public chat room broadcast.
* User join/leave notifications broadcast.
* Dynamic online user list broadcast.
* Duplicate username prevention on connection.
* User-specific error messages (e.g., for username taken).
* Basic backend exception handling for message processing.

## 2. Project Setup

The project follows standard Spring Boot conventions using Gradle (Kotlin DSL).

* **Prerequisites:** JDK 17+ and Gradle.
* **Structure:** Standard layout (`src/main/kotlin`, `src/main/resources`).
* **Build:** `build.gradle.kts` manages dependencies. Key dependencies include `spring-boot-starter-websocket` and `jackson-module-kotlin`. The `kotlin-spring` plugin is used.
* **Running:** Use `./gradlew bootRun` or run the `ChatApplication.kt` main class from an IDE.

## 3. Configuration (`WebSocketConfig.kt`)

This class configures the WebSocket and STOMP communication layer.

* **`@EnableWebSocketMessageBroker`:** Enables broker-backed STOMP messaging.
* **Message Broker (`configureMessageBroker`)**:
    * Uses a **Simple In-Memory Broker** for destinations prefixed with `/topic` (e.g., `/topic/public`, `/topic/users`). Suitable for single-instance deployments; requires an external broker (like RabbitMQ/Redis) for horizontal scaling.
    * Sets `/app` as the **Application Destination Prefix**. Messages sent by clients to destinations like `/app/chat.sendMessage` are routed to `@MessageMapping` methods in controllers.
* **STOMP Endpoints (`registerStompEndpoints`)**:
    * Registers `/ws-chat` as the HTTP endpoint for the initial WebSocket handshake.
    * Uses `.withSockJS()` to enable fallback transports for wider client compatibility.
    * Uses `.setAllowedOriginPatterns("*")` for development CORS flexibility (MUST be restricted to specific frontend origins in production for security).

## 4. Core Components

* **`ChatMessage.kt` (Model):** A Kotlin `data class` defining the message structure, including `type` (enum: `CHAT`, `JOIN`, `LEAVE`, `ERROR`), `sender` (String), `content` (String?), and `recipient` (String?). Jackson handles JSON serialization.
* **`ChatController.kt` (Controller):**
    * Handles incoming STOMP messages directed to `/app/...` destinations using `@MessageMapping`.
    * `sendMessage` (`/app/chat.sendMessage`): Receives `CHAT` messages, validates basic content, and uses `@SendTo("/topic/public")` to broadcast the message.
    * `addUser` (`/app/chat.addUser`): Handles `JOIN` requests. Checks for duplicate usernames using `ChatUserService.isUserOnline()`. If duplicate, sends an error message back to the specific user via `SimpMessagingTemplate.convertAndSendToUser(username, "/queue/errors", ...)`. If unique, stores the username in the WebSocket session attributes (via `SimpMessageHeaderAccessor`) for later identification (e.g., on disconnect), adds the user via `ChatUserService`, and manually broadcasts the `JOIN` message to `/topic/public` and the updated user list to `/topic/users` using `SimpMessagingTemplate`.
    * Includes a basic `@MessageExceptionHandler` to catch errors during message processing and send an error message back to the originating user via `@SendToUser("/queue/errors", ...)`.
* **`ChatUserService.kt` (Service):**
    * Manages the state of online users.
    * Uses a thread-safe `Set` (backed by `ConcurrentHashMap`) to store usernames, ensuring safe concurrent access.
    * Provides methods: `addUser`, `removeUser`, `isUserOnline`, `getOnlineUsers` (returns an immutable copy).
* **`WebSocketEventListener.kt` (Listener):**
    * Listens for Spring's `SessionDisconnectEvent` using `@EventListener`.
    * Retrieves the `username` associated with the disconnected session from the session attributes (set by `ChatController`).
    * Calls `ChatUserService.removeUser(username)`.
    * Broadcasts a `LEAVE` message (with null content) to `/topic/public` and the updated user list to `/topic/users` using `SimpMessagingTemplate`.

## 5. Communication Flow Summary

1.  **Connect:** Client connects to `/ws-chat` (HTTP -> WebSocket/SockJS -> STOMP CONNECT).
2.  **Join:** Client subscribes to `/topic/public`, `/topic/users`, `/user/queue/errors`. Sends `JOIN` message to `/app/chat.addUser`. Controller validates, adds user, stores username in session, broadcasts `JOIN` to `/topic/public`, broadcasts user list to `/topic/users`. (Or sends error to `/user/queue/errors` if username taken).
3.  **Send Chat:** Client sends `CHAT` message to `/app/chat.sendMessage`. Controller receives, `@SendTo` broadcasts to `/topic/public`.
4.  **Leave:** Connection drops. Server fires `SessionDisconnectEvent`. `WebSocketEventListener` catches it, gets username from session, calls `removeUser`, broadcasts `LEAVE` to `/topic/public`, broadcasts user list to `/topic/users`.

## 6. API Reference (STOMP Destinations)

* **Client Sends To:**
    * `/app/chat.addUser` (Payload: `ChatMessage` type `JOIN`)
    * `/app/chat.sendMessage` (Payload: `ChatMessage` type `CHAT`)
* **Client Subscribes To:**
    * `/topic/public` (Receives: `ChatMessage` type `CHAT`, `JOIN`, `LEAVE`)
    * `/topic/users` (Receives: JSON Array of online usernames `String[]`)

## 7. Future Enhancements

* **Security:** Integrate Spring Security for authentication/authorization.
* **Persistence:** Store messages/users in a database (Spring Data).
* **Private Messaging:** Implement 1-to-1 chat using user destinations.
* **Typing Indicators:** Add real-time typing notifications.
* **Testing:** Implement comprehensive unit and integration tests.
* **Scalability:** Use an external message broker (RabbitMQ, Redis) instead of `SimpleBroker` for multi-instance deployments.
* **Configuration:** Externalize more settings (topic names, CORS origins).

This summary provides a solid overview of the backend's architecture, components, and communication patterns.
