package com.eventstream.utils;

import com.eventstream.core.EventStreamProcessor;

import java.time.Instant;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Мониторинг производительности системы
 * Отслеживает ключевые метрики в реальном времени
 */
public class PerformanceMonitor {
    
    private final EventStreamProcessor processor;
    private final ConcurrentHashMap<String, MetricTracker> metrics;
    private final AtomicReference<Instant> startTime;
    private final AtomicLong totalEventsProcessed;
    private final AtomicLong totalRollbacks;
    private final AtomicLong totalRollbackTime;
    
    public PerformanceMonitor(EventStreamProcessor processor) {
        this.processor = processor;
        this.metrics = new ConcurrentHashMap<>();
        this.startTime = new AtomicReference<>(Instant.now());
        this.totalEventsProcessed = new AtomicLong(0);
        this.totalRollbacks = new AtomicLong(0);
        this.totalRollbackTime = new AtomicLong(0);
    }
    
    /**
     * Начинает отслеживание метрики
     */
    public void startMetric(String metricName) {
        metrics.put(metricName, new MetricTracker(Instant.now()));
    }
    
    /**
     * Завершает отслеживание метрики
     */
    public void endMetric(String metricName) {
        MetricTracker tracker = metrics.get(metricName);
        if (tracker != null) {
            tracker.end();
        }
    }
    
    /**
     * Отслеживает обработку события
     */
    public void trackEventProcessing() {
        totalEventsProcessed.incrementAndGet();
    }
    
    /**
     * Отслеживает откат состояния
     */
    public void trackRollback(long durationMs) {
        totalRollbacks.incrementAndGet();
        totalRollbackTime.addAndGet(durationMs);
    }
    
    /**
     * Возвращает текущую статистику производительности
     */
    public PerformanceStats getCurrentStats() {
        Instant now = Instant.now();
        Duration uptime = Duration.between(startTime.get(), now);
        
        long eventsProcessed = totalEventsProcessed.get();
        long rollbacks = totalRollbacks.get();
        long totalRollbackTimeMs = totalRollbackTime.get();
        
        double eventsPerSecond = uptime.getSeconds() > 0 ? 
            (double) eventsProcessed / uptime.getSeconds() : 0;
        double eventsPerMinute = eventsPerSecond * 60;
        
        double avgRollbackTime = rollbacks > 0 ? 
            (double) totalRollbackTimeMs / rollbacks : 0;
        
        return new PerformanceStats(
            uptime,
            eventsProcessed,
            eventsPerSecond,
            eventsPerMinute,
            rollbacks,
            avgRollbackTime,
            metrics
        );
    }
    
    /**
     * Сбрасывает все метрики
     */
    public void reset() {
        startTime.set(Instant.now());
        totalEventsProcessed.set(0);
        totalRollbacks.set(0);
        totalRollbackTime.set(0);
        metrics.clear();
    }
    
    /**
     * Выводит текущую статистику в консоль
     */
    public void printStats() {
        PerformanceStats stats = getCurrentStats();
        System.out.println("\n📊 Статистика производительности:");
        System.out.println("=================================");
        System.out.printf("⏱️  Время работы: %s\n", formatDuration(stats.uptime()));
        System.out.printf("📈 Обработано событий: %,d\n", stats.eventsProcessed());
        System.out.printf("⚡ Событий в секунду: %.2f\n", stats.eventsPerSecond());
        System.out.printf("🚀 Событий в минуту: %.2f\n", stats.eventsPerMinute());
        System.out.printf("🔄 Количество откатов: %,d\n", stats.rollbacks());
        System.out.printf("⏳ Среднее время отката: %.2f мс\n", stats.avgRollbackTime());
        
        if (!stats.metrics().isEmpty()) {
            System.out.println("\n📋 Детальные метрики:");
            stats.metrics().forEach((name, tracker) -> {
                if (tracker.isCompleted()) {
                    System.out.printf("  %s: %d мс\n", name, tracker.getDurationMs());
                }
            });
        }
        
        // Проверяем целевые показатели
        System.out.println("\n🎯 Целевые показатели:");
        System.out.printf("  >1M событий/мин: %s\n", 
            stats.eventsPerMinute() >= 1000000 ? "✅" : "❌");
        System.out.printf("  <100мс откат: %s\n", 
            stats.avgRollbackTime() < 100 ? "✅" : "❌");
    }
    
    /**
     * Форматирует длительность для вывода
     */
    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        
        if (hours > 0) {
            return String.format("%dч %02dм %02dс", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dм %02dс", minutes, seconds);
        } else {
            return String.format("%dс", seconds);
        }
    }
    
    /**
     * Статистика производительности
     */
    public record PerformanceStats(
        Duration uptime,
        long eventsProcessed,
        double eventsPerSecond,
        double eventsPerMinute,
        long rollbacks,
        double avgRollbackTime,
        ConcurrentHashMap<String, MetricTracker> metrics
    ) {}
    
    /**
     * Трекер метрики
     */
    public static class MetricTracker {
        private final Instant startTime;
        private Instant endTime;
        
        public MetricTracker(Instant startTime) {
            this.startTime = startTime;
        }
        
        public void end() {
            this.endTime = Instant.now();
        }
        
        public boolean isCompleted() {
            return endTime != null;
        }
        
        public long getDurationMs() {
            if (endTime == null) {
                return Duration.between(startTime, Instant.now()).toMillis();
            }
            return Duration.between(startTime, endTime).toMillis();
        }
    }
}