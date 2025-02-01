package logbook.javafx;

import java.util.Map;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
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
    
    @Override
    public void start(Stage primaryStage) {
        LogBookKaiMessageFlowConfig config = LogBookKaiMessageFlowConfig.getInstance();
        portLabel = new Label("起動Port:" + config.getKoukainissikaiMessageFlowPort() + " 航海日誌改Port" + config.getKoukainissikaiPort());

        webSocketStatusLabel = new Label();
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
        
        VBox root = new VBox(10);
        root.getChildren().addAll(portLabel, webSocketStatusLabel, queueStatusLabel);

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
    }
    

    private void updateQueueStatus() {
        Map<String, Integer> totalCount = QueueStatus.getQueueTotalCounts();
        
        int apiCount = totalCount.getOrDefault(QueueName.API.getQueueName(), 0);
        int imageCount = totalCount.getOrDefault(QueueName.IMAGE.getQueueName(), 0);
        int imageJsonCount = totalCount.getOrDefault(QueueName.IMAGEJSON.getQueueName(), 0);

        queueStatusLabel.setText(String.format("Queue実行数(直近5分間) API : %d Image : %d ImageJson : %d",
                apiCount, imageCount, imageJsonCount));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
