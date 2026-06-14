/**
 * WebSocket / STOMP messaging support.
 *
 * <p>Planned classes:
 * <ul>
 *   <li><b>OrderStatusMessage</b>   – STOMP message payload published when an order status changes</li>
 *   <li><b>WebSocketEventPublisher</b> – service that sends messages to {@code /topic/kitchen}
 *                                        and {@code /topic/table/{tableId}} via {@code SimpMessagingTemplate}</li>
 * </ul>
 *
 * <p>The STOMP broker is configured in {@code WebSocketConfig}.
 * JavaScript clients subscribe to topics using SockJS + stomp.js.
 */
package com.restaurant.rms.websocket;
