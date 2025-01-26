package logbook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import logbook.queue.QueueHolder;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);
    
    /**
     * https://docs.spring.io/spring-framework/reference/web/websocket/server.html#websocket-server-runtime-configuration
     * @return
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(32 * 1024 * 1024);
        container.setMaxBinaryMessageBufferSize(32 * 1024 * 1024);
        return container;
    }
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ApiWebSocketHandler(), "/api").setAllowedOriginPatterns("*")
                                                                                                    .withSockJS()
                                                                                                        .setHeartbeatTime(30000) //30s
                                                                                                        .setWebSocketEnabled(true)
                                                                                                        .setStreamBytesLimit(32 * 1024 * 1024); //32M
        
        registry.addHandler(new ImageWebSocketHandler(), "/image").setAllowedOriginPatterns("*")
                                                                                                    .withSockJS()
                                                                                                        .setHeartbeatTime(30000) //30s
                                                                                                        .setWebSocketEnabled(true)
                                                                                                        .setStreamBytesLimit(32 * 1024 * 1024); //32M
                                                                                                        
        registry.addHandler(new ImageJsonWebSocketHandler(), "/imageJson").setAllowedOriginPatterns("*")
                                                                                                    .withSockJS()
                                                                                                        .setHeartbeatTime(30000) //30s
                                                                                                        .setWebSocketEnabled(true)
                                                                                                        .setStreamBytesLimit(32 * 1024 * 1024); //32M
                                                                                                        
    }
    
    private static class ApiWebSocketHandler extends MyWebSocketHandler {
        @Override
        public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            String payload = message.getPayload();
            logger.info("api received message size : " + payload.length());

            // シャットダウン中ならスキップ
            if (QueueHolder.getInstance().isShuttingDown()) {
                logger.warn("API queue is shutting down. Skipping message processing.");
                logger.warn("api received message : " + payload);
                session.sendMessage(new TextMessage("shutdown"));
                return;
            }
            
            //Queueに登録
            QueueHolder.getInstance().getAPIQueue().offer(payload);
            
            session.sendMessage(new TextMessage("ok"));
        }
    }

    private static class ImageWebSocketHandler extends MyWebSocketHandler {
        @Override
        public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            String payload = message.getPayload();
            logger.info("image received message size : " + payload.length());
            
            // シャットダウン中ならスキップ
            if (QueueHolder.getInstance().isShuttingDown()) {
                logger.warn("Image queue is shutting down. Skipping message processing.");
                logger.warn("Image received message : " + payload);
                session.sendMessage(new TextMessage("shutdown"));
                return;
            }
            
            //Queueに登録
            QueueHolder.getInstance().getImageQueue().offer(payload);
            
            session.sendMessage(new TextMessage("ok"));
        }
    }

    private static class ImageJsonWebSocketHandler extends MyWebSocketHandler {
        @Override
        public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            String payload = message.getPayload();
            logger.info("received message size : " + payload.length());

            // シャットダウン中ならスキップ
            if (QueueHolder.getInstance().isShuttingDown()) {
                logger.warn("ImageJson queue is shutting down. Skipping message processing.");
                logger.warn("ImageJson received message : " + payload);
                session.sendMessage(new TextMessage("shutdown"));
                return;
            }
            
            //Queueに登録
            QueueHolder.getInstance().getImageJsonQueue().offer(payload);

            session.sendMessage(new TextMessage("ok"));
        }
    }
    
    private static class MyWebSocketHandler extends TextWebSocketHandler {

        @Override
        public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            String payload = message.getPayload();
            logger.info("received message size : " + payload.length());
            
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
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
            logger.error("Transport error: " + exception.getMessage());
        }

    }
}