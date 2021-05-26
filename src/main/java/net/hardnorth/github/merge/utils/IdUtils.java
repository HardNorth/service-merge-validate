package net.hardnorth.github.merge.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class IdUtils {
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

    private static final Map<Thread, Integer> THREAD_NUMBERS = new ConcurrentHashMap<>();

    public static String generateId() {
        return System.currentTimeMillis() + "-"
                + THREAD_NUMBERS.computeIfAbsent(Thread.currentThread(), t -> THREAD_COUNTER.incrementAndGet()) + "-"
                + ThreadLocalRandom.current().nextInt(9999);
    }
}
