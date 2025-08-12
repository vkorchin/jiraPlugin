package com.eventprocessing.handler;

import com.eventprocessing.model.Event;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Реестр обработчиков событий
 * Обеспечивает потокобезопасное управление handlers для разных типов событий
 */
public class EventHandlerRegistry {
    
    private final Map<String, EventHandler> handlers = new ConcurrentHashMap<>();
    
    /**
     * Регистрирует обработчик для определенного типа события
     */
    public void registerHandler(EventHandler handler) {
        handlers.put(handler.getEventType(), handler);
    }
    
    /**
     * Удаляет обработчик для определенного типа события
     */
    public void unregisterHandler(String eventType) {
        handlers.remove(eventType);
    }
    
    /**
     * Получает обработчик для определенного типа события
     */
    public EventHandler getHandler(String eventType) {
        return handlers.get(eventType);
    }
    
    /**
     * Получает обработчик для события
     */
    public EventHandler getHandler(Event event) {
        return getHandler(event.getEventType());
    }
    
    /**
     * Проверяет, зарегистрирован ли обработчик для данного типа события
     */
    public boolean hasHandler(String eventType) {
        return handlers.containsKey(eventType);
    }
    
    /**
     * Проверяет, зарегистрирован ли обработчик для данного события
     */
    public boolean hasHandler(Event event) {
        return hasHandler(event.getEventType());
    }
    
    /**
     * Возвращает количество зарегистрированных обработчиков
     */
    public int getHandlerCount() {
        return handlers.size();
    }
    
    /**
     * Очищает все обработчики
     */
    public void clear() {
        handlers.clear();
    }
}