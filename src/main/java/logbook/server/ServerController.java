package logbook.server;

import java.io.IOException;
import java.util.Base64;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import logbook.cache.CacheHolder;
import logbook.queue.QueueName;

/**
 * 航海日誌改からのProxy通信を受ける、サーバーとして稼働する機能です
 */
@RestController
public class ServerController {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerController.class);
    
    // `/kcsapi/`配下のAPIを処理
    @RequestMapping(value = "/kcsapi/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> handleApiRequests(HttpServletRequest request) {
        String hashKey = request.getHeader("x-koukainissikai");
        if (logger.isDebugEnabled()) {
            request.getHeaderNames();
            logger.debug("Request検知 : " + request.getRequestURI());
            logger.debug("Request Headers:");
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                logger.debug("  {}: {}", headerName, headerValue);
            }
        }
        CacheHolder<String, String> cacheHolder = CacheHolder.getInstance(QueueName.API);
        String responseBody = cacheHolder.get(hashKey);
        if (responseBody == null) {
            logger.warn("Cache miss for key: " + hashKey);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("No data found : " + hashKey);   
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentLength(responseBody.length());
        
        return ResponseEntity.ok()
                   .header(HttpHeaders.CONNECTION, "close")
                   .contentType(MediaType.TEXT_PLAIN)
                   .headers(headers)
                   .body(responseBody);
    }
    
    @GetMapping(value = "/kcs2/**")
    public ResponseEntity<?> handleResourcesRequests(HttpServletRequest request) throws IOException {
        String hashKey = request.getHeader("x-koukainissikai");
        String sendType = request.getHeader("x-koukainissikai-sendtype");
        if (logger.isDebugEnabled()) {
            request.getHeaderNames();
            logger.debug("Request検知 : " + request.getRequestURI());
            logger.debug("Request Headers:");
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                logger.debug("  {}: {}", headerName, headerValue);
            }
        }
        CacheHolder<String, String> cacheHolder = CacheHolder.getInstance(QueueName.IMAGE);
        String responseBody = cacheHolder.get(hashKey);
        if (responseBody == null) {
            logger.warn("Cache miss for key: " + hashKey);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("No data found : " + hashKey);   
        }
        
        if ("image".equals(sendType)) {
            // Base64 文字列をバイト配列にデコード
            byte[] imageBytes = Base64.getDecoder().decode(responseBody);

            // HTTP ヘッダーを設定
            HttpHeaders headers = new HttpHeaders();
            headers.setContentLength(imageBytes.length);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONNECTION, "close")
                    .contentType(MediaType.IMAGE_PNG)
                    .headers(headers)
                    .body(imageBytes);
        } else if ("json".equals(sendType)) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentLength(responseBody.length());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONNECTION, "close")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers)
                    .body(responseBody);
            
        }
        return ResponseEntity.ok().body("");
    }
    
}
