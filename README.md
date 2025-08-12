# Система потоковой обработки событий с откатом состояний

Высокопроизводительная Java система для обработки потоковых событий в реальном времени с возможностью отката состояний сущностей к любому моменту времени.

## 🚀 Ключевые возможности

- **Потоковая обработка**: Обработка миллионов событий в минуту
- **Откат состояний**: Быстрый откат к любому моменту времени (< 100 мс)
- **Потокобезопасность**: Полная thread-safety для параллельной обработки
- **Расширяемость**: Легкое добавление новых типов событий
- **Оптимизация**: Система snapshot'ов для ускорения откатов
- **Стратегии отката**: Множественные стратегии с автовыбором оптимальной

## 🏗️ Архитектура

### Основные компоненты

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   EventStream   │───▶│  EventProcessor  │───▶│  StateManager   │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │                        │
                                ▼                        ▼
                       ┌──────────────────┐    ┌─────────────────┐
                       │ EventHandlerRegistry│    │ SnapshotManager │
                       └──────────────────┘    └─────────────────┘
                                │                        │
                                ▼                        ▼
                       ┌──────────────────┐    ┌─────────────────┐
                       │  RollbackService │◀───│   Rollbacks     │
                       └──────────────────┘    └─────────────────┘
```

### Модели данных

- **Event**: Базовое событие с entityId, timestamp, eventType, payload
- **Entity**: Сущность с состоянием (copy-on-write)
- **Snapshot**: Снимок состояния сущности в определенный момент времени

### Менеджеры

- **StateManager**: Управление текущими состояниями и историей событий
- **SnapshotManager**: Создание и управление снимками для оптимизации
- **EventHandlerRegistry**: Реестр обработчиков событий

## 🎯 Стратегии отката

1. **FAST_SNAPSHOT_ONLY**: Использует только ближайший snapshot (самый быстрый)
2. **SNAPSHOT_PLUS_EVENTS**: Snapshot + события после него (оптимальный)
3. **FULL_EVENT_REPLAY**: Перепроигрывание всех событий (самый точный)

## 📦 Структура проекта

```
src/main/java/com/eventprocessing/
├── model/              # Модели данных
│   ├── Event.java
│   ├── Entity.java
│   └── Snapshot.java
├── stream/             # Потоки событий
│   ├── EventStream.java
│   └── InMemoryEventStream.java
├── handler/            # Обработчики событий
│   ├── EventHandler.java
│   ├── EventHandlerRegistry.java
│   └── impl/           # Конкретные обработчики
├── state/              # Управление состояниями
│   └── StateManager.java
├── snapshot/           # Управление снимками
│   └── SnapshotManager.java
├── processor/          # Основной процессор
│   └── EventProcessor.java
├── rollback/           # Сервис откатов
│   └── RollbackService.java
└── demo/               # Демонстрационные примеры
    └── EventProcessingDemo.java
```

## 🚀 Быстрый старт

### 1. Создание и запуск системы

```java
// Создание компонентов
EventProcessor processor = new EventProcessor();
InMemoryEventStream eventStream = new InMemoryEventStream();

// Регистрация обработчиков событий
processor.registerEventHandler(new CreateEventHandler());
processor.registerEventHandler(new UpdateEventHandler());
processor.registerEventHandler(new DeleteEventHandler());
processor.registerEventHandler(new StatusChangeEventHandler());

// Запуск системы
processor.start(eventStream);
```

### 2. Отправка событий

```java
// Создание сущности
eventStream.pushEvent("user123", "CREATE", Map.of(
    "name", "John Doe",
    "email", "john@example.com",
    "status", "active"
));

// Обновление сущности
eventStream.pushEvent("user123", "UPDATE", Map.of(
    "email", "john.doe@example.com"
));

// Изменение статуса
eventStream.pushEvent("user123", "STATUS_CHANGE", Map.of(
    "status", "inactive"
));
```

### 3. Получение состояния

```java
// Текущее состояние
Entity currentState = processor.getCurrentState("user123");

// Откат к определенному времени
RollbackService rollbackService = new RollbackService(
    processor.stateManager, 
    processor.snapshotManager, 
    processor.handlerRegistry
);

CompletableFuture<RollbackResult> future = rollbackService.rollbackToTime(
    "user123", 
    Instant.now().minus(1, ChronoUnit.HOURS),
    RollbackStrategy.SNAPSHOT_PLUS_EVENTS
);

RollbackResult result = future.get();
Entity pastState = result.getEntity();
```

### 4. Создание собственного обработчика событий

```java
public class CustomEventHandler implements EventHandler {
    @Override
    public String getEventType() {
        return "CUSTOM_EVENT";
    }
    
    @Override
    public Entity handle(Event event, Entity currentEntity) {
        Entity entity = currentEntity != null ? currentEntity : new Entity(event.getEntityId());
        
        // Ваша логика обработки
        Map<String, Object> updates = new HashMap<>(event.getPayload());
        updates.put("processedAt", Instant.now());
        
        return entity.withUpdatedState(updates, event.getTimestamp());
    }
}

// Регистрация
processor.registerEventHandler(new CustomEventHandler());
```

## 📊 Производительность

### Целевые показатели
- **Обработка событий**: > 1,000,000 событий/минуту
- **Время отката**: < 100 мс для большинства случаев
- **Потокобезопасность**: Полная поддержка параллельной обработки

### Оптимизации
- **Copy-on-write** для состояний сущностей
- **ConcurrentHashMap** и **ConcurrentSkipListMap** для thread-safety
- **ReadWriteLock** для минимизации блокировок
- **Автоматические snapshot'ы** каждые N событий
- **Умный выбор стратегии** отката

## 🧪 Тестирование

### Запуск демонстрации
```bash
java -cp target/classes com.eventprocessing.demo.EventProcessingDemo
```

### Запуск тестов
```bash
java -ea -cp target/classes com.eventprocessing.EventProcessingTest
```

## 🔧 Конфигурация

### SnapshotManager
```java
// Snapshot каждые 50 событий, максимум 20 snapshot'ов на сущность
SnapshotManager snapshotManager = new SnapshotManager(50, 20);
```

### EventProcessor
```java
// Пул потоков размером 8
EventProcessor processor = new EventProcessor(8);
```

## 🛡️ Обработка ошибок и отказоустойчивость

- **Graceful degradation**: Система продолжает работать при ошибках обработки отдельных событий
- **Exception isolation**: Ошибки в одном обработчике не влияют на другие
- **Automatic recovery**: Автоматическое восстановление после сбоев
- **Memory management**: Автоматическая очистка старых событий и snapshot'ов

## 🎯 Решение подводных камней

### 1. Гонка состояний
- **ReadWriteLock** для каждой сущности
- **Copy-on-write** подход для состояний
- **Атомарные операции** для критических секций

### 2. Управление памятью
- **Автоматическая очистка** старых событий
- **Лимиты на количество** snapshot'ов
- **Lazy loading** для больших объемов данных

### 3. Неупорядоченные события
- **Сортировка по timestamp** в ConcurrentSkipListMap
- **Буферизация** для обработки поздних событий
- **Конфигурируемые окна** ожидания

### 4. Расширяемость
- **Паттерн Strategy** для обработчиков событий
- **Реестр обработчиков** для динамической регистрации
- **Интерфейсы** для легкого добавления новых компонентов

## 📈 Мониторинг и метрики

```java
EventProcessor.ProcessingStats stats = processor.getStats();
System.out.println("Обработано событий: " + stats.processedEvents);
System.out.println("Количество сущностей: " + stats.entityCount);
System.out.println("Количество snapshot'ов: " + stats.snapshotCount);
System.out.println("Среднее время обработки: " + stats.avgProcessingTimeMs + " мс");
System.out.println("Среднее время отката: " + stats.avgRollbackTimeMs + " мс");
```

## 🤝 Расширение системы

Система спроектирована для легкого расширения:

1. **Новые типы событий**: Создайте EventHandler
2. **Новые источники событий**: Реализуйте EventStream
3. **Новые стратегии отката**: Расширьте RollbackService
4. **Новые типы хранилищ**: Замените StateManager

## 📄 Лицензия

Этот проект создан в образовательных целях и демонстрирует лучшие практики разработки высокопроизводительных систем обработки событий на Java.
