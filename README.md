# EventStreamProcessor - Система потоковой обработки событий с откатом состояний

## Описание

EventStreamProcessor - это высокопроизводительная система на Java для обработки непрерывного потока событий с возможностью отката состояний сущностей на любую произвольную временную точку.

## Архитектура

### Основные компоненты

- **EventStreamProcessor** - главный процессор событий
- **EventStore** - хранилище событий с быстрым поиском по времени
- **SnapshotManager** - менеджер снимков состояний для оптимизации откатов
- **EventProcessor** - интерфейс для обработчиков различных типов событий
- **Entity** - представление состояния сущности
- **Event** - структура события

### Ключевые особенности

✅ **Потокобезопасность** - использование `ConcurrentHashMap`, `ConcurrentSkipListMap`, `ReadWriteLock`  
✅ **Высокая производительность** - целевые >1M событий/мин  
✅ **Быстрые откаты** - целевые <100мс  
✅ **Расширяемость** - добавление новых типов событий без изменения базового кода  
✅ **Оптимизация памяти** - автоматическое создание и управление снимками  
✅ **Только стандартные библиотеки Java** - `java.util.concurrent`, `java.time`  

## Структура проекта

```
src/main/java/com/eventstream/
├── core/                    # Основные классы системы
│   ├── Event.java          # Модель события
│   ├── Entity.java         # Модель сущности
│   ├── EventStreamProcessor.java  # Главный процессор
│   ├── EventStream.java    # Интерфейс потока событий
│   └── SimulatedEventStream.java # Симуляция потока для тестов
├── handlers/               # Обработчики событий
│   ├── EventProcessor.java # Интерфейс обработчика
│   ├── DefaultEventProcessor.java # Базовый обработчик
│   └── UserEventProcessor.java    # Специализированный обработчик
├── storage/                # Хранение данных
│   ├── EventStore.java     # Хранилище событий
│   ├── SnapshotManager.java # Менеджер снимков
│   └── EntitySnapshot.java # Снимок состояния
├── utils/                  # Утилиты
│   └── PerformanceTest.java # Тесты производительности
└── Demo.java               # Демонстрация работы
```

## Быстрый старт

### 1. Создание процессора

```java
// Создаем процессор с настройками: снимок каждые 100 событий, максимум 10 снимков
EventStreamProcessor processor = new EventStreamProcessor(100, 10);

// Регистрируем обработчики
processor.registerEventProcessor(new UserEventProcessor());
```

### 2. Обработка событий

```java
// Создаем событие
Event event = new Event(
    "user-001",                    // ID сущности
    Instant.now(),                 // Временная метка
    "user.created",                // Тип события
    Map.of("name", "John Doe"),    // Данные события
    0                              // Номер последовательности (автоматически)
);

// Обрабатываем событие
processor.processEvent(event);
```

### 3. Откат состояния

```java
// Получаем состояние сущности на определенное время
Instant targetTime = Instant.now().minusHours(1);
Entity historicalState = processor.getEntityStateAt("user-001", targetTime);
```

### 4. Подключение к потоку событий

```java
// Создаем поток событий
SimulatedEventStream eventStream = new SimulatedEventStream(1000); // 1000 событий/сек

// Подписываемся на события
eventStream.subscribe(processor::processEvent);

// Запускаем поток
eventStream.start();
```

## Расширение системы

### Добавление нового типа события

```java
public class OrderEventProcessor implements EventProcessor {
    
    @Override
    public boolean canHandle(String eventType) {
        return eventType.startsWith("order.");
    }
    
    @Override
    public void handle(Event event, Entity entity) {
        // Логика обработки заказов
        switch (event.getEventType()) {
            case "order.created":
                handleOrderCreated(event, entity);
                break;
            case "order.paid":
                handleOrderPaid(event, entity);
                break;
        }
    }
    
    @Override
    public int getPriority() {
        return 30; // Приоритет выше базового
    }
}

// Регистрация
processor.registerEventProcessor(new OrderEventProcessor());
```

## Производительность

### Целевые показатели

- **Обработка событий**: >1,000,000 событий/мин
- **Время отката**: <100мс
- **Потокобезопасность**: 100% при параллельной обработке

### Оптимизации

1. **Снимки состояний** - автоматическое создание каждые N событий
2. **ConcurrentSkipListMap** - быстрый поиск по временным меткам O(log n)
3. **Асинхронная обработка** - пул потоков для обработки событий
4. **Copy-on-write** - безопасное копирование состояний
5. **ReadWriteLock** - минимизация блокировок при чтении

## Запуск демонстрации

```bash
# Компиляция
javac -cp . src/main/java/com/eventstream/**/*.java

# Запуск демонстрации
java -cp src/main/java com.eventstream.Demo

# Запуск тестов производительности
java -cp src/main/java com.eventstream.utils.PerformanceTest
```

## Тестирование

### Тест производительности

```java
PerformanceTest test = new PerformanceTest();
test.runPerformanceTest();        // Полный тест
test.testConcurrency();           // Тест многопоточности
```

### Тест откатов

```java
// Тестируем откат на разные временные точки
Instant[] testTimestamps = {
    Instant.now().minusHours(1),
    Instant.now().minusMinutes(30),
    Instant.now().minusMinutes(5)
};

for (Instant timestamp : testTimestamps) {
    long startTime = System.nanoTime();
    Entity entity = processor.getEntityStateAt("user-001", timestamp);
    long durationMs = (System.nanoTime() - startTime) / 1_000_000;
    
    System.out.printf("Откат на %s: %d мс %s\n", 
        timestamp, durationMs, durationMs < 100 ? "✅" : "❌");
}
```

## Мониторинг и статистика

```java
// Получение статистики системы
String stats = processor.getStatistics();
System.out.println(stats);

// Пример вывода:
// EventStreamProcessor: 15000 processed events, 25 entities, 
// EventStore: 25 entities, 15000 total events, 
// SnapshotManager: 25 entities, 150 total snapshots
```

## Управление памятью

```java
// Очистка старых данных
Instant cutoffTime = Instant.now().minusDays(7);
processor.cleanupOldData(cutoffTime);

// Настройка частоты снимков
EventStreamProcessor processor = new EventStreamProcessor(
    50,   // Снимок каждые 50 событий
    20    // Максимум 20 снимков на сущность
);
```

## Обработка ошибок

Система автоматически обрабатывает ошибки в обработчиках событий:

```java
// Обработка событий происходит в try-catch блоке
try {
    processor.handle(event, entity);
} catch (Exception e) {
    System.err.println("Error processing event: " + e.getMessage());
    // Событие не теряется, но логируется ошибка
}
```

## Безопасность

- **Потокобезопасность**: все операции с общими данными защищены
- **Атомарность**: операции с состояниями атомарны
- **Изоляция**: каждый поток работает с копией состояния
- **Восстановление**: система продолжает работу после ошибок

## Масштабирование

### Горизонтальное масштабирование

```java
// Создание нескольких процессоров для разных доменов
EventStreamProcessor userProcessor = new EventStreamProcessor();
EventStreamProcessor orderProcessor = new EventStreamProcessor();

// Каждый процессор может работать независимо
userProcessor.registerEventProcessor(new UserEventProcessor());
orderProcessor.registerEventProcessor(new OrderEventProcessor());
```

### Вертикальное масштабирование

```java
// Настройка пула потоков
EventStreamProcessor processor = new EventStreamProcessor() {
    {
        // Переопределяем пул потоков
        this.eventProcessingExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2
        );
    }
};
```

## Лучшие практики

1. **Регистрируйте обработчики** в порядке приоритета
2. **Используйте снимки** для оптимизации откатов
3. **Мониторьте производительность** через статистику
4. **Очищайте старые данные** для экономии памяти
5. **Тестируйте многопоточность** перед продакшеном

## Ограничения

- **Память**: все события хранятся в памяти (для продакшена рекомендуется персистентное хранилище)
- **Время**: максимальное время отката зависит от количества событий
- **Масштаб**: количество сущностей ограничено доступной памятью

## Планы развития

- [ ] Персистентное хранение событий (Kafka, Redis)
- [ ] Распределенная архитектура (Apache Kafka, Apache Flink)
- [ ] Веб-интерфейс для мониторинга
- [ ] Метрики и алерты (Prometheus, Grafana)
- [ ] Поддержка схем событий (Apache Avro, Protocol Buffers)

## Лицензия

MIT License - свободное использование для любых целей.
