package logbook;

import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextClosedEvent;

import javafx.application.Platform;
import javafx.stage.Stage;
import logbook.config.LogBookKaiMessageFlowConfig;
import logbook.javafx.LogBookMessageFlowView;
import logbook.queue.ApiConsumer;
import logbook.queue.ImageConsumer;
import logbook.queue.ImageJsonConsumer;
import logbook.queue.QueueHolder;
import logbook.queue.QueueName;

@SpringBootApplication
public class WebSocketServerApplication implements ApplicationListener<ContextClosedEvent> {

    private static ConfigurableApplicationContext context;
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketServerApplication.class);
    private CountDownLatch shutdownLatch;
    private ExecutorService apiExecutorService;
    private ExecutorService imageExecutorService;
    private ExecutorService imageJsonExecutorService;
    
    /**
     * -Dport MessageFlowのport番号 : 未指定時 8890 
     * -Dkaiport 航海日誌改のport番号 : 未指定時 8888
     */
    public static void main(String[] args) {
        // Javaの引数からポート番号を取得
        LogBookKaiMessageFlowConfig config = LogBookKaiMessageFlowConfig.getInstance();
        String port = System.getProperty("port");
        if (port != null && port.matches("\\d+")) {
            System.setProperty("server.port", port);
            config.setKoukainissikaiMessageFlowPort(Integer.parseInt(port));
        }
        String logBookPort = System.getProperty("kaiport");
        if (logBookPort != null && logBookPort.matches("\\d+")) {
            config.setKoukainissikaiPort(Integer.parseInt(logBookPort));
        }

        
        // JavaFXを起動
        Platform.startup(() -> {
            Stage stage = new Stage();
            LogBookMessageFlowView application = new LogBookMessageFlowView();
            application.start(stage);

            // JavaFXウィンドウが閉じられた際の処理
            stage.setOnCloseRequest(event -> {
                // Spring Bootアプリケーションをシャットダウン
                if (context != null) {
                    SpringApplication.exit(context);
                }
                Platform.exit(); // JavaFXプラットフォームを終了
                System.exit(0); // アプリケーション全体を終了
            });
        });
        
        context = SpringApplication.run(WebSocketServerApplication.class, args);
    }

    @Bean
    public CommandLineRunner run() {
        return args -> {
            // Queue 起動
            QueueHolder queue = QueueHolder.getInstance();

            // 通信順序を維持するため、SingleThreadExecutor を使用
            this.apiExecutorService = Executors.newSingleThreadExecutor();
            this.imageExecutorService = Executors.newSingleThreadExecutor();
            this.imageJsonExecutorService = Executors.newSingleThreadExecutor();

            // Consumer をスレッドプールで実行
            this.apiExecutorService.execute(new ApiConsumer(queue.getAPIQueue(), QueueName.API));
            this.imageExecutorService.execute(new ImageConsumer(queue.getImageQueue(), QueueName.IMAGE));
            this.imageJsonExecutorService.execute(new ImageJsonConsumer(queue.getImageJsonQueue(), QueueName.IMAGEJSON));

            //シャットダウン用Latch初期化
            this.shutdownLatch = new CountDownLatch(1);
        };
    }
    
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        logger.info("シャットダウン処理を開始します。");
        QueueHolder queue = QueueHolder.getInstance();
        queue.setShuttingDown(true);

        try {
            safelyShutdown(this.apiExecutorService, queue.getAPIQueue(), QueueName.API);
        } catch (Exception e) {
            logger.error("APIQueue シャットダウン処理中にエラー発生", e);
        }
        try {
            safelyShutdown(this.imageExecutorService, queue.getImageQueue(), QueueName.IMAGE);
        } catch (Exception e) {
            logger.error("ImageQueue シャットダウン処理中にエラー発生", e);
        }
        try {
            safelyShutdown(this.imageJsonExecutorService, queue.getImageJsonQueue(), QueueName.IMAGEJSON);
        } catch (Exception e) {
            logger.error("ImageJsonQueue シャットダウン処理中にエラー発生", e);
        }

        shutdownLatch.countDown();//シャットダウン処理完了を通知
        logger.info("シャットダウン処理が完了しました。");
    }
    
    /**
     * キューの状態を500ms * 60回 = 30s監視し、Queueが捌けているのを確認してから終了します
     * 
     * @param executorService
     * @param queue
     * @param queueName
     */
    private void safelyShutdown(ExecutorService executorService, Queue<String> queue, QueueName queueName) {
        int attempts = 0;
        boolean isQueueEmpty = false;

        while (attempts < 60) { // 最大 60 回試行
            isQueueEmpty = queue.isEmpty();
            if (isQueueEmpty) {
                logger.info(queueName + " が空になりました。安全にシャットダウンします。");
                break;
            }
            try {
                logger.info(queueName + " にデータが残っています。待機中... (" + (attempts + 1) + "/20)");
                Thread.sleep(500); // 500ms 待機
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            attempts++;
        }

        if (isQueueEmpty) {
            executorService.shutdown();
        } else {
            logger.info(queueName + " にデータが残っていますが強制終了します。");
            executorService.shutdownNow();
        }

        try {
            if (executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.info(queueName + " のシャットダウンが正常終了しました。");
            } else {
                logger.info(queueName + " のシャットダウンがタイムアウトしました。強制終了します。");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.info(queueName + " のシャットダウン中に割り込まれました。強制終了します。");
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info(queueName + " の ExecutorService をシャットダウンしました。");
    }
}