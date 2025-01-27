package logbook.server;

import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import logbook.cache.CacheHolder;

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
        CacheHolder<String, String> cacheHolder = CacheHolder.getInstance();
        String responseBody = cacheHolder.get(hashKey);
        
        return ResponseEntity.ok()
                   .contentType(MediaType.TEXT_PLAIN)
                   .body(responseBody);
    }
}
