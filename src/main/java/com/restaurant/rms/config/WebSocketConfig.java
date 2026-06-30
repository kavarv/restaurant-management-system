package com.restaurant.rms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * STOMP-over-WebSocket configuration.
 *
 * <h3>Destination prefix semantics</h3>
 * <ul>
 *   <li><strong>/topic/*</strong> — broadcast (pub-sub). One message is fanned out to every
 *       subscriber. Used for shared feeds: kitchen display, floor manager's table grid,
 *       and per-order watchers.</li>
 *   <li><strong>/queue/*</strong> — point-to-point (unicast). The broker routes to a single
 *       named queue. Combined with the user-destination prefix, the server can push to a
 *       specific logged-in user without exposing the raw session ID.</li>
 *   <li><strong>/user/*</strong> prefix ({@code setUserDestinationPrefix}) — Spring rewrites
 *       {@code /user/{username}/queue/notifications} into an internal session-scoped queue
 *       for that user. The client subscribes to {@code /user/queue/notifications} and Spring
 *       automatically scopes it to their session.</li>
 * </ul>
 *
 * <h3>SockJS fallback</h3>
 * <p>SockJS transparently falls back to HTTP long-polling / XHR-streaming for browsers or
 * corporate proxies that block WebSocket upgrades. The STOMP API is identical regardless
 * of the underlying transport.</p>
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${websocket.allowed-origins:http://localhost:8080}")
    private String allowedOrigins;

    /**
     * Registers {@code /ws} as the STOMP handshake endpoint with SockJS fallback.
     * Clients connect via {@code new SockJS('/ws')} in the browser.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins.split(","))
                .withSockJS();
    }

    /**
     * <ul>
     *   <li>{@code enableSimpleBroker("/topic", "/queue")} — in-memory broker handles
     *       all destinations starting with /topic (broadcast) and /queue (unicast).</li>
     *   <li>{@code setApplicationDestinationPrefixes("/app")} — client frames with
     *       destination /app/... are dispatched to {@code @MessageMapping} methods.</li>
     *   <li>{@code setUserDestinationPrefix("/user")} — enables
     *       {@code simpMessagingTemplate.convertAndSendToUser(username, "/queue/...", payload)}
     *       for server-initiated per-user pushes.</li>
     * </ul>
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");