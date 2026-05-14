package logbook;

import java.io.StringReader;

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

import jakarta.json.Json;
import jakarta.json.JsonReader;
import logbook.queue.QueueHolder;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);
    private static final Logger trafficLogger = LoggerFactory.getLogger("logbook.traffic");

    /**
     * https://docs.spring.io/spring-framework/reference/web/websocket/server.html#websocket-server-runtime-configuration
     * 
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
                .setHeartbeatTime(20000) // 20s
                .setWebSocketEnabled(true)
                .setStreamBytesLimit(32 * 1024 * 1024); // 32M

        registry.addHandler(new ImageWebSocketHandler(), "/image").setAllowedOriginPatterns("*")
                .withSockJS()
                .setHeartbeatTime(20000) // 20s
                .setWebSocketEnabled(true)
                .setStreamBytesLimit(32 * 1024 * 1024); // 32M

        registry.addHandler(new ImageJsonWebSocketHandler(), "/imageJson").setAllowedOriginPatterns("*")
                .withSockJS()
                .setHeartbeatTime(20000) // 20s
                .setWebSocketEnabled(true)
                .setStreamBytesLimit(32 * 1024 * 1024); // 32M

    }

    private static class ApiWebSocketHandler extends MyWebSocketHandler {
        public ApiWebSocketHandler() {
            super(WebSocketStatus.API);
        }

        @Override
        public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            String payload = message.getPayload();
            if (logger.isDebugEnabled()) {
                logger.debug("api received message size : " + payload.length());
            }

            // シャットダウン中ならスキップ
            if (QueueHolder.getInstance().isShuttingDown()) {
                logger.warn("API queue is shutting down. Skipping message processing.");
                logger.warn("api received message : " + payload);
                session.sendMessage(new TextMessage("shutdown"));
                return;
            }

            // 通信を受け取った時間を追加
            payload = addReceivedTime(payload);

            String uri = "unknown";
            try (JsonReader reader = Json.createReader(new StringReader(payload))) {
                uri = reader.readObject().getString("uri", "unknown");
            } catch (Exception e) {
                // ignore
            }

            // Queueに登録
            QueueHolder.getInstance().getAPIQueue().offer(payload);

            trafficLogger.info("WebSocket受信(API): " + uri + " 受信バイト数 : " + payload.length());
            session.sendMessage(new TextMessage("ok"));
        }
    }

    private static class ImageWebSocketHandler extends MyWebSocketHandler {
        public ImageWebSocketHandler() {
            super(WebSocketStatus.IMAGE);
        }

        @Override
        public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            String payload = message.getPayload();
            if (logger.isDebugEnabled()) {
                logger.debug("image received message size : " + payload.length());
            }

            // シャットダウン中ならスキップ
            if (QueueHolder.getInstance().isShuttingDown()) {
                logger.warn("Image queue is shutting down. Skipping message processing.");
                logger.warn("Image received message : " + payload);
                session.sendMessage(new TextMessage("shutdown"));
                return;
            }

            // 通信を受け取った時間を追加
            payload = addReceivedTime(payload);

            String uri = "unknown";
            try (JsonReader reader = Json.createReader(new StringReader(payload))) {
                uri = reader.readObject().getString("uri", "unknown");
            } catch (Exception e) {
                // ignore
            }

            // Queueに登録
            QueueHolder.getInstance().getImageQueue().offer(payload);

            trafficLogger.info("WebSocket受信(Image): " + uri + " 受信バイト数 : " + payload.length());
            session.sendMessage(new TextMessage("ok"));
        }
    }

    private static class ImageJsonWebSocketHandler extends MyWebSocketHandler {
        public ImageJsonWebSocketHandler() {
            super(WebSocketStatus.IMAGE_JSON);
        }

        @Override
        public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            String payload = message.getPayload();
            if (logger.isDebugEnabled()) {
                logger.debug("received message size : " + payload.length());
            }

            // シャットダウン中ならスキップ
            if (QueueHolder.getInstance().isShuttingDown()) {
                logger.warn("ImageJson queue is shutting down. Skipping message processing.");
                logger.warn("ImageJson received message : " + payload);
                session.sendMessage(new TextMessage("shutdown"));
                return;
            }

            // 通信を受け取った時間を追加
            payload = addReceivedTime(payload);

            String uri = "unknown";
            try (JsonReader reader = Json.createReader(new StringReader(payload))) {
                uri = reader.readObject().getString("uri", "unknown");
            } catch (Exception e) {
                // ignore
            }

            // Queueに登録
            QueueHolder.getInstance().getImageJsonQueue().offer(payload);

            trafficLogger.info("WebSocket受信(ImageJson): " + uri + " 受信バイト数 : " + payload.length());
            session.sendMessage(new TextMessage("ok"));
        }
    }

    private static class MyWebSocketHandler extends TextWebSocketHandler {

        private final WebSocketStatus status;

        public MyWebSocketHandler(WebSocketStatus status) {
            this.status = status;
        }

        @Override
        public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            String payload = message.getPayload();
            if (logger.isDebugEnabled()) {
                logger.debug("received message size : " + payload.length());
            }

            session.sendMessage(new TextMessage("ok"));
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            status.increment();
            logger.info("Connection established: " + session.getId());
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status)
                throws Exception {
            this.status.decrement();
            logger.info("Connection closed: " + session.getId() + ", Status: " + status);
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
            logger.error("Transport error: " + exception.getMessage());
        }

    }

    /**
     * payloadに受信時間を追加する
     */
    private static String addReceivedTime(String payload) {
        if (payload == null) {
            return null;
        }
        String trimmed = payload.trim();
        if (trimmed.endsWith("}")) {
            return trimmed.substring(0, trimmed.length() - 1) + ",\"receivedTime\":" + System.currentTimeMillis() + "}";
        }
        return payload;
    }
}