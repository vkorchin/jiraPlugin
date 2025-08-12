package com.eventstream.handlers;

import com.eventstream.core.Entity;
import com.eventstream.core.Event;

/**
 * Интерфейс для обработки событий определенного типа
 */
public interface EventHandler {
    
    /**
     * Проверяет, может ли данный обработчик обработать событие указанного типа
     */
    boolean canHandle(String eventType);
    
    /**
     * Обрабатывает событие и обновляет состояние сущности
     */
    Entity handle(Event event, Entity currentEntity);
    
    /**
     * Откатывает событие, восстанавливая предыдущее состояние
     */
    Entity rollback(Event event, Entity currentEntity);
}