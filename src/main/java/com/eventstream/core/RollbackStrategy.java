package com.eventstream.core;

/**
 * Стратегии отката состояний сущностей
 */
public enum RollbackStrategy {
    
    /**
     * Быстрый откат по ближайшему снимку + события
     * Самый быстрый, но может быть менее точным
     */
    FAST_SNAPSHOT("Быстрый откат по снимку"),
    
    /**
     * Точный откат по всем событиям
     * Самый точный, но может быть медленнее
     */
    PRECISE_EVENTS("Точный откат по событиям"),
    
    /**
     * Гибридный подход: снимок + события с ограничением
     * Баланс между скоростью и точностью
     */
    HYBRID("Гибридный откат"),
    
    /**
     * Адаптивный откат: выбор стратегии на основе данных
     * Автоматически выбирает лучшую стратегию
     */
    ADAPTIVE("Адаптивный откат");
    
    private final String description;
    
    RollbackStrategy(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return name() + " (" + description + ")";
    }
}