package logbook;

import java.util.concurrent.atomic.AtomicInteger;

public class WebSocketStatus {
    public static final WebSocketStatus API = new WebSocketStatus("API");
    public static final WebSocketStatus IMAGE = new WebSocketStatus("Image");
    public static final WebSocketStatus IMAGE_JSON = new WebSocketStatus("ImageJson");

    private final String name;
    private final AtomicInteger connectionCount = new AtomicInteger(0);

    private WebSocketStatus(String name) {
        this.name = name;
    }

    public void increment() {
        connectionCount.incrementAndGet();
    }

    public void decrement() {
        connectionCount.decrementAndGet();
    }

    public int getCount() {
        return connectionCount.get();
    }

    public String getName() {
        return name;
    }
}