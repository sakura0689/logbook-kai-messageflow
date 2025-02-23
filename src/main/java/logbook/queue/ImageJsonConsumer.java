package logbook.queue;

import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.json.JsonObject;
import logbook.cache.CacheHolder;
import logbook.config.LogBookKaiMessageFlowConfig;
import logbook.utils.DateTimeUtils;
import logbook.webClient.WebClientConfig;

public class ImageJsonConsumer extends BaseConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ImageJsonConsumer.class);

    public ImageJsonConsumer(Queue<String> queue, QueueName queueName) {
        super(queue, queueName);
    }

    protected void sendData(JsonObject json) {
        String method = getJsonToString(json, "method");
        String uri = getJsonToString(json, "uri");

        if ("GET".equals(method)) {
            String responseBody = getJsonToString(json, "responseBody");

            String hashKey = getQueueName() + DateTimeUtils.getCurrentTimestamp();
            CacheHolder<String, String> cacheHolder = CacheHolder.getInstance(QueueName.IMAGEJSON);
            cacheHolder.put(hashKey, responseBody);

            WebClient webClient = WebClientConfig.createCustomWebClient();
            String response = webClient.get()
                    .uri(uri)
                    .header("User-Agent", hashKey) //航海日誌改のProxy内で上書きされるがデバック用
                    .header("Origin", LogBookKaiMessageFlowConfig.getInstance().getKoukainissikaiMessageFlowOrigin())
                    .header("Host", LogBookKaiMessageFlowConfig.getInstance().getKoukainissikaiMessageFlowHost())
                    .header("Proxy-Connection", "keep-alive")
                    .header("x-koukainissikai", hashKey)
                    .header("x-koukainissikai-sendtype", "json")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            if (logger.isDebugEnabled()) {
                logger.debug("response : " + response);
            }
        }
    }

}
