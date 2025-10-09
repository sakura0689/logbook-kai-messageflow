package logbook.queue;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class QueueHolder {
    private static QueueHolder instance;

    private final Queue<String> apiQueue = new ConcurrentLinkedQueue<>();
    private final Queue<String> apiPortQueue = new ConcurrentLinkedQueue<>();
    private final Queue<String> imageQueue = new ConcurrentLinkedQueue<>();
    private final Queue<String> imageJsonQueue = new ConcurrentLinkedQueue<>();

    private QueueHolder() {}

    //shutdownフラグ
    private volatile boolean isShuttingDown = false;
    
    public static QueueHolder getInstance() {
        if (instance == null) {
            instance = new QueueHolder();
        }
        return instance;
    }

    public Queue<String> getAPIQueue() {
        return apiQueue;
    }

    public Queue<String> getAPIPortQueue() {
        return apiPortQueue;
    }

    public Queue<String> getImageQueue() {
        return imageQueue;
    }

    public Queue<String> getImageJsonQueue() {
        return imageJsonQueue;
    }
    
    public boolean isShuttingDown() {
        return isShuttingDown;
    }

    public void setShuttingDown(boolean shuttingDown) {
        this.isShuttingDown = shuttingDown;
    }
}
