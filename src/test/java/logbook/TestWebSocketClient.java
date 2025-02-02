package logbook;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

public class TestWebSocketClient {

    private static final Logger logger = LoggerFactory.getLogger(TestWebSocketClient.class);

    public static void main(String[] args) {
        // SockJSエンドポイントのURL
        String sockJsUrl = "http://localhost:8890/api";

        // Transportを定義 (WebSocketを使用)
        List<Transport> transports = Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()));
        // SockJSクライアントを作成
        SockJsClient sockJsClient = new SockJsClient(transports);

        // CountDownLatchを生成 (初期カウントは1)
        CountDownLatch latch = new CountDownLatch(1);
        
        // SockJSの接続を開始
        WebSocketConnectionManager connectionManager = new WebSocketConnectionManager(sockJsClient, new TextWebSocketHandler() {
            @Override
            public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                String payload = message.getPayload();
                logger.info("Received message: " + payload);

                // メッセージを受信したらカウントをデクリメント
                latch.countDown();
            }

            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                logger.info("Connection established: " + session.getId());

                // メッセージを送信
                String sendStr = "## test message ##";
                session.sendMessage(new TextMessage(sendStr));
                
                sendStr = "## test message2 ##";
                session.sendMessage(new TextMessage(sendStr));
                logger.info("Message sent to server.");
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status)
                    throws Exception {
                logger.info("Connection closed: " + session.getId() + ", Status: " + status);
            }
        }, sockJsUrl);
        connectionManager.start();

        logger.info("SockJS client started. Connecting to: " + sockJsUrl);
        
        // カウントが0になるまで待機 (タイムアウトを設定することも可能)
        boolean awaitResult = true;
        try {
            awaitResult = latch.await(20, TimeUnit.SECONDS); // 20秒タイムアウト
        } catch (InterruptedException e) {
            logger.error("Timeout occurred while waiting for server response.", e);
        }

        if (!awaitResult) {
            logger.error("Timeout occurred while waiting for server response.");
        } else {
            logger.info("Received response from server. Exiting.");
        }
        connectionManager.stop();
    }
}