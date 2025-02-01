package logbook.queue;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class QueueStatus {
    private static final Map<String, Integer> queueCounts = new ConcurrentHashMap<>();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HHmm");

    private static Pattern pattern = Pattern.compile("(.+?)_\\d{4}"); // キュー名と時刻の間の "_" を区切り文字として抽出
    
    public static synchronized void incrementQueueCount(String queueName) {
        String key = getCurrentMinuteKey(queueName);
        queueCounts.merge(key, 1, Integer::sum);
        
        String thresholdKey = getThresholdMinuteKey(queueName);
        if (queueCounts.containsKey(thresholdKey)) {
            cleanupOldEntries(thresholdKey);
        }
    }

    public static synchronized void ensureCurrentMinuteExists(String queueName) {
        String key = getCurrentMinuteKey(queueName);
        queueCounts.putIfAbsent(key, 0);
        
        String thresholdKey = getThresholdMinuteKey(queueName);
        if (queueCounts.containsKey(thresholdKey)) {
            cleanupOldEntries(thresholdKey);
        }
    }

    private static void cleanupOldEntries(String thresholdKey) {
        queueCounts.keySet().removeIf(key -> key.compareTo(thresholdKey) < 0);
    }
    
    private static String getCurrentMinuteKey(String queueName) {
        return queueName + "_" + LocalDateTime.now().format(FORMATTER);
    }
    
    private static String getThresholdMinuteKey(String queueName) {
        return queueName + "_" + LocalDateTime.now().minusMinutes(5).format(FORMATTER);
    }
    
    public static synchronized Map<String, Integer> getQueueCounts() {
        return new ConcurrentHashMap<>(queueCounts);
    }
    
    public static Map<String, Integer> getQueueTotalCounts() {
        Map<String, Integer> totalCounts = queueCounts.entrySet().stream()
                .collect(Collectors.groupingBy(
                        entry -> extractQueueName(entry.getKey()),
                        Collectors.summingInt(Map.Entry::getValue)
                ));
        return totalCounts;
    }

    private static String extractQueueName(String key) {
        Matcher matcher = pattern.matcher(key);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return "";
        }
    }
}
