package logbook.server;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class ServerController {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerController.class);
    
    // `/kcsapi/`配下のAPIを処理
    @RequestMapping(value = "/kcsapi/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> handleApiRequests(HttpServletRequest request) {
        logger.debug("Request検知 : " + request.getRequestURI());
        return ResponseEntity.ok().body(Map.of("status", "ok"));
    }
}
