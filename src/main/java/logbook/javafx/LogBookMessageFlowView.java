package logbook.javafx;

import java.util.Map;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import logbook.WebSocketStatus;
import logbook.config.LogBookKaiMessageFlowConfig;
import logbook.queue.QueueName;
import logbook.queue.QueueStatus;

public class LogBookMessageFlowView extends Application {
    
    private Label portLabel;
    private Label webSocketStatusLabel;
    private Label queueStatusLabel;
    private Circle webSocketStatusCircle;
    private Timeline blinkTimeline; // 点滅アニメーション
    private boolean isInitConnect = false;
    
    @Override
    public void start(Stage primaryStage) {
        LogBookKaiMessageFlowConfig config = LogBookKaiMessageFlowConfig.getInstance();
        portLabel = new Label("起動Port:" + config.getKoukainissikaiMessageFlowPort() + " 航海日誌改Port" + config.getKoukainissikaiPort());

        webSocketStatusLabel = new Label();
        webSocketStatusCircle = new Circle(6);
        updateWebSocketStatus(); // 初回表示
        
        queueStatusLabel = new Label();
        updateQueueStatus(); // 初回表示
        
        // 5秒ごとにWebSocketの接続数,直近5分のQueue実行数を更新
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(5), 
                        event -> {
                            updateWebSocketStatus();
                            updateQueueStatus();
                        })
                );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        
        HBox webSocketStatusBox = new HBox(10, webSocketStatusLabel, webSocketStatusCircle);
        webSocketStatusBox.setAlignment(Pos.CENTER_LEFT);
        
        VBox root = new VBox(10);
        root.getChildren().addAll(portLabel, webSocketStatusBox, queueStatusLabel);
        root.setAlignment(Pos.CENTER_LEFT);
        
        Scene scene = new Scene(root, 400, 100);
        primaryStage.setScene(scene);
        primaryStage.setTitle("航海日誌改 MessageFlow");
        primaryStage.show();
    }

    private void updateWebSocketStatus() {
        int apiCount = WebSocketStatus.API.getCount();
        int imageCount = WebSocketStatus.IMAGE.getCount();
        int imageJsonCount = WebSocketStatus.IMAGE_JSON.getCount();

        webSocketStatusLabel.setText(String.format("WebSocket 接続数 API : %d Image : %d ImageJson : %d",
                apiCount, imageCount, imageJsonCount));
        
        if (apiCount > 0 || imageCount > 0 || imageJsonCount > 0) {
            webSocketStatusCircle.setFill(Color.GREEN);
            isInitConnect = true;
            stopBlinking();
        } else {
            if (isInitConnect) {
                startBlinking();
            } else {
                webSocketStatusCircle.setFill(Color.RED); 
            }
        }
    }
    

    private void updateQueueStatus() {
        Map<String, Integer> totalCount = QueueStatus.getQueueTotalCounts();
        
        int apiCount = totalCount.getOrDefault(QueueName.API.getQueueName(), 0);
        int imageCount = totalCount.getOrDefault(QueueName.IMAGE.getQueueName(), 0);
        int imageJsonCount = totalCount.getOrDefault(QueueName.IMAGEJSON.getQueueName(), 0);

        queueStatusLabel.setText(String.format("Queue実行数(直近5分間) API : %d Image : %d ImageJson : %d",
                apiCount, imageCount, imageJsonCount));
    }

    /**
     * アイコンの点滅処理
     */
    private void startBlinking() {
        if (blinkTimeline == null) {
            blinkTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1.0), e -> webSocketStatusCircle.setFill(Color.RED)),
                new KeyFrame(Duration.seconds(1.5), e -> webSocketStatusCircle.setFill(Color.TRANSPARENT))
            );
            blinkTimeline.setCycleCount(Timeline.INDEFINITE);
        }
        blinkTimeline.play();
    }

    /**
     * 点滅停止処理
     */
    private void stopBlinking() {
        if (blinkTimeline != null) {
            blinkTimeline.stop();
            webSocketStatusCircle.setFill(Color.GREEN);
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
