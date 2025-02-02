package logbook.queue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QueueStatus {
    private static final Map<String, List<String>> apiQueueCounts = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> imageQueueCounts = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> imageJsonQueueCounts = new ConcurrentHashMap<>();
    private static final DateTimeFormatter FORMATTER_HHmm = DateTimeFormatter.ofPattern("HHmm");
    private static final DateTimeFormatter FORMATTER_HHmmssSSS = DateTimeFormatter.ofPattern("HHmmssSSS");

    public static synchronized void incrementQueueCount(String queueName) {
        LocalDateTime dateTime = LocalDateTime.now();
        String key = dateTime.format(FORMATTER_HHmm);
        String incrementTime = dateTime.format(FORMATTER_HHmmssSSS);
        
        QueueName queue = QueueName.getQueue(queueName);
        if (QueueName.API.equals(queue)) {
            if (!apiQueueCounts.containsKey(key)) {
                apiQueueCounts.put(key,new ArrayList<String>());
            }
            apiQueueCounts.get(key).add(incrementTime);
            
            String thresholdKey = getThresholdMinuteKey(dateTime);
            if (apiQueueCounts.containsKey(thresholdKey)) {
                apiQueueCounts.keySet().removeIf(k -> k.compareTo(thresholdKey) < 0);
            }
        } else if (QueueName.IMAGE.equals(queue)) {
            if (!imageQueueCounts.containsKey(key)) {
                imageQueueCounts.put(key,new ArrayList<String>());
            }
            imageQueueCounts.get(key).add(incrementTime);
            
            String thresholdKey = getThresholdMinuteKey(dateTime);
            if (imageQueueCounts.containsKey(thresholdKey)) {
                imageQueueCounts.keySet().removeIf(k -> k.compareTo(thresholdKey) < 0);
            }
        } else if (QueueName.IMAGEJSON.equals(queue)) {
            if (!imageJsonQueueCounts.containsKey(key)) {
                imageJsonQueueCounts.put(key,new ArrayList<String>());
            }
            imageJsonQueueCounts.get(key).add(incrementTime);
            
            String thresholdKey = getThresholdMinuteKey(dateTime);
            if (imageJsonQueueCounts.containsKey(thresholdKey)) {
                imageJsonQueueCounts.keySet().removeIf(k -> k.compareTo(thresholdKey) < 0);
            }
        }
    }

    public static synchronized void ensureCurrentMinuteExists(String queueName) {
        LocalDateTime dateTime = LocalDateTime.now();
        String key = dateTime.format(FORMATTER_HHmm);
        
        QueueName queue = QueueName.getQueue(queueName);
        if (QueueName.API.equals(queue)) {
            if (!apiQueueCounts.containsKey(key)) {
                apiQueueCounts.put(key,new ArrayList<String>());
            }
            
            String thresholdKey = getThresholdMinuteKey(dateTime);
            if (apiQueueCounts.containsKey(thresholdKey)) {
                apiQueueCounts.keySet().removeIf(k -> k.compareTo(thresholdKey) < 0);
            }
        } else if (QueueName.IMAGE.equals(queue)) {
            if (!imageQueueCounts.containsKey(key)) {
                imageQueueCounts.put(key,new ArrayList<String>());
            }
            
            String thresholdKey = getThresholdMinuteKey(dateTime);
            if (imageQueueCounts.containsKey(thresholdKey)) {
                imageQueueCounts.keySet().removeIf(k -> k.compareTo(thresholdKey) < 0);
            }
        } else if (QueueName.IMAGEJSON.equals(queue)) {
            if (!imageJsonQueueCounts.containsKey(key)) {
                imageJsonQueueCounts.put(key,new ArrayList<String>());
            }
            
            String thresholdKey = getThresholdMinuteKey(dateTime);
            if (imageJsonQueueCounts.containsKey(thresholdKey)) {
                imageJsonQueueCounts.keySet().removeIf(k -> k.compareTo(thresholdKey) < 0);
            }
        }
    }

    private static String getThresholdMinuteKey(LocalDateTime dateTime) {
        return dateTime.minusMinutes(5).format(FORMATTER_HHmm);
    }
        
    public static Map<String, Integer> getQueueTotalCounts() {
        Map<String, Integer> totalCounts = new HashMap<>();
        
        int apiCount = apiQueueCounts.values().stream().mapToInt(List::size).sum();
        int imageCount = imageQueueCounts.values().stream().mapToInt(List::size).sum();
        int imageJsonCount = imageJsonQueueCounts.values().stream().mapToInt(List::size).sum();
        
        totalCounts.put(QueueName.API.getQueueName(), apiCount);
        totalCounts.put(QueueName.IMAGE.getQueueName(), imageCount);
        totalCounts.put(QueueName.IMAGEJSON.getQueueName(), imageJsonCount);
        return totalCounts;
    }
}
