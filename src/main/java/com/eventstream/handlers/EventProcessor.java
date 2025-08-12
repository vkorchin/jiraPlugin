package com.eventstream.handlers;

import com.eventstream.core.Entity;
import com.eventstream.core.Event;

/**
 * Интерфейс для обработчиков событий
 * Позволяет добавлять новые типы событий без модификации существующего кода
 */
public interface EventProcessor {
    
    /**
     * Проверяет, может ли данный обработчик обработать событие
     */
    boolean canHandle(String eventType);
    
    /**
     * Обрабатывает событие и обновляет состояние сущности
     */
    void handle(Event event, Entity entity);
    
    /**
     * Возвращает приоритет обработчика (меньше = выше приоритет)
     */
    default int getPriority() {
        return 100;
    }
}