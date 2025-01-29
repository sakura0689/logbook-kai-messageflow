package logbook.queue;

import java.io.StringReader;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import logbook.cache.CacheHolder;
import logbook.config.LogBookKaiMessageFlowConfig;
import logbook.utils.DateTimeUtils;
import logbook.webClient.WebClientConfig;

public class BaseConsumer implements Runnable {
    
    private static final Logger logger = LoggerFactory.getLogger(BaseConsumer.class);
    
    private final String queueName;
    
    private final Queue<String> queue;
    
    private volatile boolean running = true;
    private volatile boolean isShutDown = false;

    public BaseConsumer(Queue<String> queue, String queueName) {
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
                    try {
                        if (logger.isDebugEnabled()) {
                            
                        }
                        StringReader reader = new StringReader(data);
                        JsonObject json;
                        try (JsonReader jsonreader = Json.createReader(reader)) {
                            json = jsonreader.readObject();
                        }
                        if (json != null) {
                            sendData(json);
                        } else {
                            logger.error("JsonのParseが出来ませんでした : " + data);
                        }
                        
                    } catch (Exception e) {
                        logger.error(queueName + " : " + data, e);
                    }
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
    
    protected void sendData(JsonObject json) {
        String method = getJsonToString(json, "method");
        String uri = getJsonToString(json, "uri");
        
        if ("POST".equals(method)) {
            String postData = getJsonToString(json, "postData");
            String responseBody = getJsonToString(json, "responseBody");
            
            String hashKey = queueName + DateTimeUtils.getCurrentTimestamp();
            CacheHolder<String, String> cacheHolder = CacheHolder.getInstance();
            cacheHolder.put(hashKey, responseBody);
            
            WebClient webClient = WebClientConfig.createCustomWebClient();
            String response = webClient.post()
                    .uri(uri)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("User-Agent", hashKey) //航海日誌改のProxy内で上書きされるがデバック用
                    .header("Origin", LogBookKaiMessageFlowConfig.getInstance().getKoukainissikaiMessageFlowOrigin())
                    .header("Host", LogBookKaiMessageFlowConfig.getInstance().getKoukainissikaiMessageFlowHost())
                    .header("Proxy-Connection", "keep-alive")
                    .header("x-koukainissikai", hashKey)
                    .bodyValue(postData)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            if (logger.isDebugEnabled()) {
                logger.debug("response : " + response);
            }
        }
    }
    
    public void stop() {
        logger.info(queueName + " Consumerのshutdownを開始");
        this.isShutDown = true;
    }
    
    protected String getJsonToString(JsonObject json, String key) {
        JsonValue jsonVal = json.get(key);
        if (jsonVal instanceof JsonString jsonString) {
            return jsonString.getString();
        }
        return jsonVal.toString();
    }
}
