package logbook.queue;

import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Consumer implements Runnable {
    
    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);
    
    private final String queueName;
    
    private final Queue<String> queue;
    
    private volatile boolean running = true;
    private volatile boolean isShutDown = false;

    public Consumer(Queue<String> queue, String queueName) {
        this.queue = queue;
        this.queueName = queueName;
    }
    
    /**
     * thread.stopか、QueueHolder.getInstance().setShuttingDown(true)の検知後、
     * Queueが空で終了します
     */
    @Override
    public void run() {
        try {
            int i = 0;
            while (running) {
                if (QueueHolder.getInstance().isShuttingDown()) {
                    logger.info(queueName + " ConsumerのShutdown処理を検知しました Shutdown処理を開始します");
                    isShutDown = true;
                }
                String data = queue.poll();
                if (data != null) {
                    logger.debug(queueName + " : " + data);
                } else {
                    if (isShutDown) {
                        //shutdownを受け取り、Queueが空なら処理終了
                        logger.info(queueName + " ConsumerのQueueが空を確認 終了します");
                        running = false;
                    } else {
                        i++;
                        if (i % 100 == 0) {
                            //ログ出力は10秒に1回にする
                            logger.debug(queueName + " is empty...");
                            i =0;
                        }
                    }
                }
                Thread.sleep(100); // 100msスリープ
            }
            logger.info(queueName + " Consumerを終了しました");
        } catch (InterruptedException e) {
            logger.error(queueName + " Consumerを強制終了しました");
            Thread.currentThread().interrupt();
        }
    }
    
    public void stop() {
        logger.info(queueName + " Consumerのshutdownを開始");
        this.isShutDown = true;
    }
}
