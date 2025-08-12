package com.eventprocessing.handler;

import com.eventprocessing.model.Entity;
import com.eventprocessing.model.Event;

/**
 * Интерфейс для обработки событий определенного типа
 * Позволяет легко добавлять новые типы событий без изменения основного кода
 */
public interface EventHandler {
    
    /**
     * Возвращает тип события, который может обработать данный handler
     */
    String getEventType();
    
    /**
     * Обрабатывает событие и возвращает обновленную сущность
     * @param event событие для обработки
     * @param currentEntity текущее состояние сущности (может быть null для новых сущностей)
     * @return обновленное состояние сущности
     */
    Entity handle(Event event, Entity currentEntity);
    
    /**
     * Проверяет, может ли данный handler обработать событие
     */
    default boolean canHandle(Event event) {
        return getEventType().equals(event.getEventType());
    }
}