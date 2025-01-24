package logbook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ApiWebSocketHandler(), "/api").setAllowedOrigins("*");
        registry.addHandler(new ImageWebSocketHandler(), "/image").setAllowedOrigins("*");
        registry.addHandler(new ImageJsonWebSocketHandler(), "/imageJson").setAllowedOrigins("*");
    }
    
    private static class ApiWebSocketHandler extends MyWebSocketHandler {
    }

    private static class ImageWebSocketHandler extends MyWebSocketHandler {
    }

    private static class ImageJsonWebSocketHandler extends MyWebSocketHandler {
    }
    
    private static class MyWebSocketHandler extends TextWebSocketHandler {

        @Override
        public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            String payload = message.getPayload();
            logger.info("received message: " + payload);
            
            session.sendMessage(new TextMessage("ok"));
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            logger.info("Connection established: " + session.getId());
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status)
                throws Exception {
            logger.info("Connection closed: " + session.getId() + ", Status: " + status);
            
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            if (container instanceof org.apache.tomcat.websocket.WsWebSocketContainer wsContainer) {
                logger.info("WebSocket Config - MaxTextMessageBufferSize: " + wsContainer.getDefaultMaxTextMessageBufferSize());
                logger.info("WebSocket Config - MaxBinaryMessageBufferSize: " + wsContainer.getDefaultMaxBinaryMessageBufferSize());
            } else {
                logger.info("WebSocket container : " + container.toString());
            }
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
            logger.error("Transport error: " + exception.getMessage());
        }

    }
}