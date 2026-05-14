package logbook.javafx;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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
    private Label webSocketElapsedTimeLabel;
    private Timeline blinkTimeline; // 点滅アニメーション
    private boolean isInitConnect = false;
    private long startTime = -1;
    private long endTime = -1;
    private Image appIcon;

    private static final Logger logger = LogManager.getLogger("logbook.traffic");
    
    // デザインのデバッグ用フラグ (格子背景やHBoxの枠線を表示)
    private static final boolean design_debug = false;

    @Override
    public void start(Stage primaryStage) {

        appIcon = new Image(getClass().getResourceAsStream("/icon64.png"));
        primaryStage.getIcons().add(appIcon);

        LogBookKaiMessageFlowConfig config = LogBookKaiMessageFlowConfig.getInstance();
        portLabel = new Label(
                "起動Port:" + config.getKoukainissikaiMessageFlowPort() + " 航海日誌改Port" + config.getKoukainissikaiPort());

        webSocketStatusLabel = new Label();
        webSocketStatusCircle = new Circle(6);
        webSocketElapsedTimeLabel = new Label("接続時間 00:00:00");
        updateWebSocketStatus(); // 初回表示
        updateWebSocketElapsedTime(); // 初回表示

        queueStatusLabel = new Label();
        updateQueueStatus(); // 初回表示

        // 5秒ごとにWebSocketの接続数,直近5分のQueue実行数を更新
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(5),
                        event -> {
                            updateWebSocketStatus();
                            updateQueueStatus();
                        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        // 1秒ごとに接続時間を更新
        Timeline elapsedTimeTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1),
                        event -> {
                            updateWebSocketElapsedTime();
                        }));
        elapsedTimeTimeline.setCycleCount(Timeline.INDEFINITE);
        elapsedTimeTimeline.play();

        HBox webSocketStatusBox = new HBox(10, webSocketStatusLabel, webSocketStatusCircle, webSocketElapsedTimeLabel);
        webSocketStatusBox.setAlignment(Pos.CENTER_LEFT);

        // ログ表示アイコン
        InputStream iconStream = getClass().getResourceAsStream("/log_icon.png");
        javafx.scene.Node logIconNode;
        if (iconStream != null) {
            ImageView imageView = new ImageView(new Image(iconStream));
            imageView.setFitWidth(24);
            imageView.setFitHeight(24);
            logIconNode = imageView;
        } else {
            // アイコンが見つからない場合の代替（青い丸に 'L'）
            logger.warn("log_icon.png が見つかりません。代替表示を使用します。");
            javafx.scene.layout.StackPane fallback = new javafx.scene.layout.StackPane();
            Circle circle = new Circle(12, Color.CORNFLOWERBLUE);
            Label label = new Label("L");
            label.setTextFill(Color.WHITE);
            label.setStyle("-fx-font-weight: bold;");
            fallback.getChildren().addAll(circle, label);
            logIconNode = fallback;
        }
        logIconNode.setPickOnBounds(true);
        logIconNode.setCursor(javafx.scene.Cursor.HAND);
        Tooltip.install(logIconNode, new Tooltip("通信ログ表示"));
        logIconNode.setOnMouseClicked(event -> showLogWindow());

        HBox topBox = new HBox(10);
        topBox.setAlignment(Pos.CENTER_LEFT);
        topBox.setMinHeight(24);
        topBox.setPrefHeight(24);
        topBox.setMaxHeight(24);
        javafx.scene.layout.Region topSpacer = new javafx.scene.layout.Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        topBox.getChildren().addAll(portLabel, topSpacer, logIconNode);

        VBox root = new VBox(8);
        root.getChildren().addAll(topBox, webSocketStatusBox, queueStatusLabel);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new javafx.geometry.Insets(10));

        // デバッグモードがONの時のみ格子背景と枠線を表示
        if (design_debug) {
            javafx.scene.canvas.Canvas gridCanvas = new javafx.scene.canvas.Canvas(20, 20);
            javafx.scene.canvas.GraphicsContext gc = gridCanvas.getGraphicsContext2D();
            gc.setStroke(Color.RED);
            gc.setLineWidth(1);
            gc.strokeLine(0, 0, 20, 0); // 上端の線
            gc.strokeLine(0, 0, 0, 20); // 左端の線
            javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            javafx.scene.image.WritableImage gridImg = gridCanvas.snapshot(params, null);

            root.setBackground(new javafx.scene.layout.Background(
                    new javafx.scene.layout.BackgroundImage(
                            gridImg,
                            javafx.scene.layout.BackgroundRepeat.REPEAT,
                            javafx.scene.layout.BackgroundRepeat.REPEAT,
                            javafx.scene.layout.BackgroundPosition.DEFAULT,
                            javafx.scene.layout.BackgroundSize.DEFAULT)));
            
            topBox.setStyle("-fx-border-color: green; -fx-border-width: 1px;");
            webSocketStatusBox.setStyle("-fx-border-color: green; -fx-border-width: 1px;");
        }

        Scene scene = new Scene(root, 420, 100);
        primaryStage.setScene(scene);
        primaryStage.setTitle("航海日誌改 MessageFlow");
        primaryStage.show();
    }

    private void showLogWindow() {
        Stage logStage = new Stage();
        logStage.setTitle("通信ログ");
        if (appIcon != null) {
            logStage.getIcons().add(appIcon);
        }

        TextArea textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 12px;");
        VBox.setVgrow(textArea, Priority.ALWAYS);

        CheckBox autoRefreshCheck = new CheckBox("自動更新");
        autoRefreshCheck.setSelected(true);

        CheckBox apiCheck = new CheckBox("API");
        apiCheck.setSelected(true);

        CheckBox resourceCheck = new CheckBox("RESOURCE");
        resourceCheck.setSelected(true);

        Button refreshBtn = new Button("更新");
        refreshBtn.setOnAction(e -> {
            try {
                // messageflow_traffic.log を読み込む
                List<String> lines = Files.readAllLines(Paths.get("logs/messageflow_traffic.log"));

                // フィルタリング
                List<String> filteredLines = lines.stream()
                        .filter(line -> {
                            boolean isApi = line.contains("/kcsapi/");
                            boolean isResource = line.contains("/kcs2/");

                            if (isApi)
                                return apiCheck.isSelected();
                            if (isResource)
                                return resourceCheck.isSelected();

                            return true; // どちらでもない場合は表示（念のため）
                        })
                        .collect(Collectors.toList());

                // 直近500行に制限（メモリ保護）
                int start = Math.max(0, filteredLines.size() - 500);
                String logContent = filteredLines.subList(start, filteredLines.size()).stream()
                        .collect(Collectors.joining("\n"));
                textArea.setText(logContent);
                textArea.setScrollTop(Double.MAX_VALUE);
                textArea.selectPositionCaret(textArea.getLength());
                textArea.deselect();
            } catch (IOException ex) {
                logger.error("ログファイルの読み込みに失敗しました", ex);
                textArea.setText("ログファイルの読み込みに失敗しました: " + ex.getMessage());
            }
        });

        // 初回ロード
        refreshBtn.fire();

        // 5秒毎に自動更新
        Timeline autoRefreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(5), e -> {
                    if (autoRefreshCheck.isSelected()) {
                        refreshBtn.fire();
                    }
                }));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();

        // ウィンドウが閉じられたらタイマーを止める
        logStage.setOnCloseRequest(e -> autoRefreshTimeline.stop());

        HBox footer = new HBox(15);
        footer.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        footer.getChildren().addAll(autoRefreshCheck, apiCheck, resourceCheck, spacer, refreshBtn);

        VBox logRoot = new VBox(10, textArea, footer);
        logRoot.setPadding(new javafx.geometry.Insets(10));
        logRoot.setAlignment(Pos.CENTER);

        Scene logScene = new Scene(logRoot, 800, 500);
        logStage.setScene(logScene);
        logStage.show();
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
            if (startTime < 0) {
                // 初回アクセス
                startTime = System.currentTimeMillis();
            } else if (startTime > -1 && endTime > -1) {
                // 再接続
                startTime = System.currentTimeMillis();
                endTime = -1;
            }
            stopBlinking();
        } else {
            if (isInitConnect) {
                if (endTime < 0) {
                    endTime = System.currentTimeMillis();
                }
                startBlinking();
            } else {
                webSocketStatusCircle.setFill(Color.RED);
            }
        }
    }

    private void updateWebSocketElapsedTime() {
        if (startTime > -1) {
            long elapsedSeconds = 0;
            if (endTime < 0) {
                elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
            } else {
                elapsedSeconds = (endTime - startTime) / 1000;
            }

            long hours = elapsedSeconds / 3600;
            long minutes = (elapsedSeconds % 3600) / 60;
            long seconds = elapsedSeconds % 60;
            webSocketElapsedTimeLabel.setText(String.format("接続時間 %02d:%02d:%02d", hours, minutes, seconds));
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
                    new KeyFrame(Duration.seconds(1.5), e -> webSocketStatusCircle.setFill(Color.TRANSPARENT)));
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
