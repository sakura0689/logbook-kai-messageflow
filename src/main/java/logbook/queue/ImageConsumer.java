package logbook.queue;

import java.util.Base64;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.json.JsonObject;
import logbook.cache.CacheHolder;
import logbook.config.LogBookKaiMessageFlowConfig;
import logbook.utils.DateTimeUtils;
import logbook.webClient.WebClientConfig;

public class ImageConsumer extends BaseConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ImageConsumer.class);

    public ImageConsumer(Queue<String> queue, QueueName queueName) {
        super(queue, queueName);
    }

    protected void sendData(JsonObject json) {
        String method = getJsonToString(json, "method");
        String uri = getJsonToString(json, "uri");

        if ("GET".equals(method)) {
            String responseBody = getJsonToString(json, "responseBody");

            String hashKey = getQueueName() + DateTimeUtils.getCurrentTimestamp();
            CacheHolder<String, String> cacheHolder = CacheHolder.getInstance(QueueName.IMAGE);
            cacheHolder.put(hashKey, responseBody);

            WebClient webClient = WebClientConfig.createCustomWebClient();
            byte[] response = webClient.get()
                    .uri(uri)
                    .header("User-Agent", hashKey) //航海日誌改のProxy内で上書きされるがデバック用
                    .header("Origin", LogBookKaiMessageFlowConfig.getInstance().getKoukainissikaiMessageFlowOrigin())
                    .header("Host", LogBookKaiMessageFlowConfig.getInstance().getKoukainissikaiMessageFlowHost())
                    .header("Proxy-Connection", "keep-alive")
                    .header("x-koukainissikai", hashKey)
                    .header("x-koukainissikai-sendtype", "image")
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
            if (logger.isDebugEnabled()) {
                String base64Response = Base64.getEncoder().encodeToString(response);
                logger.debug("response (Base64) : " + base64Response);
            }
        }
    }

}
