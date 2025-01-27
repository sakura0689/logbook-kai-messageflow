package logbook.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CacheHolder<K, V> {

    private static final int MAX_ENTRIES = 10;
    private final ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<K> accessQueue = new ConcurrentLinkedQueue<>();

    private static final CacheHolder<?, ?> INSTANCE = new CacheHolder<>();

    private CacheHolder() {}

    @SuppressWarnings("unchecked")
    public static <K, V> CacheHolder<K, V> getInstance() {
        return (CacheHolder<K, V>) INSTANCE;
    }

    public synchronized void put(K key, V value) {
        if (cache.size() >= MAX_ENTRIES) {
            K oldestKey = accessQueue.poll();
            if (oldestKey != null) {
                cache.remove(oldestKey);
            }
        }
        accessQueue.offer(key);
        cache.put(key, value);
    }

    public V get(K key) {
        return cache.get(key);
    }

    public synchronized ConcurrentHashMap<K, V> getAll() {
        return new ConcurrentHashMap<>(cache);
    }
}