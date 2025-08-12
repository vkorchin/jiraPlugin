package com.eventprocessing.stream;

import com.eventprocessing.model.Event;

import java.util.function.Consumer;

/**
 * Интерфейс для подключения к потоку событий
 */
public interface EventStream {
    
    /**
     * Подписывается на поток событий
     * @param eventConsumer функция обработки событий
     */
    void subscribe(Consumer<Event> eventConsumer);
    
    /**
     * Останавливает поток событий
     */
    void stop();
    
    /**
     * Проверяет, активен ли поток
     * @return true если поток активен
     */
    boolean isRunning();
}