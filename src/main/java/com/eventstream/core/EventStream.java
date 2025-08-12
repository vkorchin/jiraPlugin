package com.eventstream.core;

import java.util.function.Consumer;

/**
 * Интерфейс для потока событий
 * Позволяет подписываться на события и обрабатывать их
 */
public interface EventStream {
    
    /**
     * Подписывается на события
     */
    void subscribe(Consumer<Event> eventHandler);
    
    /**
     * Отписывается от событий
     */
    void unsubscribe(Consumer<Event> eventHandler);
    
    /**
     * Запускает поток событий
     */
    void start();
    
    /**
     * Останавливает поток событий
     */
    void stop();
    
    /**
     * Проверяет, активен ли поток
     */
    boolean isActive();
}