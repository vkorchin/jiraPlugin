# Event Stream Processor

Высокопроизводительная система потоковой обработки событий с возможностью отката состояний на Java.

## 🚀 Особенности

- **Высокая производительность**: Обработка миллионов событий в минуту
- **Быстрый откат состояний**: < 100 мс для большинства операций
- **Потокобезопасность**: Полная поддержка многопоточной обработки
- **Расширяемость**: Легкое добавление новых типов событий
- **Оптимизация памяти**: Механизм снимков состояний (snapshots)
- **Copy-on-Write**: Иммутабельные структуры данных
- **Только стандартные библиотеки Java**: Без внешних зависимостей

## 🏗️ Архитектура

### Основные компоненты

1. **EventStream** - Абстракция источника событий
2. **EventProcessor** - Основной движок обработки
3. **StateManager** - Управление состояниями сущностей
4. **SnapshotManager** - Оптимизация откатов через снимки
5. **RollbackService** - Сервис отката состояний
6. **EventHandlerRegistry** - Реестр обработчиков событий

### Модель данных

```java
class Event {
    String entityId;
    Instant timestamp;
    String eventType;
    Map<String, Object> payload;
    long sequenceNumber;
}

class EntityState {
    String entityId;
    Map<String, Object> properties;
    Instant lastUpdated;
    long version;
}
```

## 🛠️ Быстрый старт

### Требования

- Java 11+
- Maven 3.6+

### Сборка проекта

```bash
# Клонирование и сборка
git clone <repository-url>
cd event-stream-processor
mvn clean compile
```

### Запуск демонстрации

```bash
# Основное демо
mvn exec:java -Pdemo

# Демо с пользовательскими обработчиками
mvn exec:java -Pcustom-demo

# Тест производительности
mvn exec:java -Pperformance
```

### Сборка исполняемого JAR

```bash
mvn clean package
java -jar target/event-stream-processor-1.0.0-jar-with-dependencies.jar
```

## 📖 Использование

### Базовая настройка

```java
// Инициализация компонентов
InMemoryEventStream eventStream = new InMemoryEventStream(1000);
StateManager stateManager = new StateManager();
SnapshotManager snapshotManager = new SnapshotManager(50);
EventHandlerRegistry handlerRegistry = new EventHandlerRegistry();

// Регистрация обработчиков
handlerRegistry.registerHandler(new CreateEntityHandler());
handlerRegistry.registerHandler(new UpdateEntityHandler());

// Создание процессора
EventProcessor eventProcessor = new EventProcessor(
    eventStream, stateManager, snapshotManager, handlerRegistry, 
    5,    // снимок каждые 5 событий
    1000  // максимум 1000 событий на сущность
);

// Создание сервиса отката
RollbackService rollbackService = new RollbackService(
    stateManager, snapshotManager, handlerRegistry, eventProcessor);
```

### Обработка событий

```java
// Запуск системы
eventProcessor.start();

// Добавление событий
Map<String, Object> payload = Map.of(
    "name", "John Doe",
    "email", "john@example.com"
);
eventStream.addEvent("user-1", "ENTITY_CREATED", payload);

// Обновление сущности
Map<String, Object> updatePayload = Map.of("age", 30);
eventStream.addEvent("user-1", "ENTITY_UPDATED", updatePayload);
```

### Откат состояний

```java
// Откат к определенному времени
Instant targetTime = Instant.now().minusSeconds(60);
Optional<EntityState> rolledBackState = 
    rollbackService.rollbackEntityState("user-1", targetTime);

// Создание временного представления (без изменения текущего состояния)
RollbackResult result = rollbackService.createRollbackView("user-1", targetTime);
System.out.println("Rollback duration: " + result.getDurationMs() + "ms");
```

### Пользовательские обработчики событий

```java
public class CustomHandler implements EventHandler {
    @Override
    public String getEventType() {
        return "CUSTOM_EVENT";
    }

    @Override
    public EntityState apply(EntityState currentState, Event event) {
        // Пользовательская логика обработки
        return currentState.withProperty("customField", 
                                       event.getPayload().get("value"), 
                                       event.getTimestamp());
    }
}

// Регистрация
handlerRegistry.registerHandler(new CustomHandler());
```

## 🔧 Конфигурация

### Параметры EventProcessor

- `snapshotFrequency` - Частота создания снимков (по умолчанию: 100 событий)
- `maxEventsPerEntity` - Максимум событий на сущность в памяти (по умолчанию: 10000)

### Параметры SnapshotManager

- `maxSnapshotsPerEntity` - Максимум снимков на сущность (по умолчанию: 100)

## 📊 Мониторинг и статистика

```java
// Статистика обработки
ProcessingStatistics stats = eventProcessor.getStatistics();
System.out.println("Processed events: " + stats.getProcessedEvents());
System.out.println("Success rate: " + stats.getSuccessRate() * 100 + "%");

// Статистика откатов
RollbackStatistics rollbackStats = rollbackService.getStatistics();
System.out.println("Snapshot coverage: " + rollbackStats.getSnapshotCoverage() * 100 + "%");
```

## 🚀 Производительность

### Целевые показатели

- **Пропускная способность**: > 1,000,000 событий/минуту
- **Время отката**: < 100 мс для большинства операций
- **Использование памяти**: Оптимизировано через снимки и очистку старых событий

### Оптимизации

1. **Снимки состояний**: Сокращают время отката
2. **Copy-on-Write**: Минимизируют копирование данных
3. **Параллельные структуры данных**: ConcurrentHashMap, ConcurrentSkipListMap
4. **Асинхронная обработка**: CompletableFuture для откатов
5. **Управление памятью**: Автоматическая очистка старых событий

## 🧪 Тестирование

```bash
# Запуск unit тестов
mvn test

# Запуск интеграционных тестов
mvn failsafe:integration-test

# Все тесты
mvn verify
```

## 🔍 Решение проблем

### Проблема гонки состояний

Система использует ReadWriteLock для каждой сущности, что предотвращает конфликты между обработкой событий и откатами.

### Управление памятью

- Автоматическая очистка старых событий при превышении лимитов
- Ограничение количества снимков на сущность
- Copy-on-Write для минимизации копирования

### Обработка неупорядоченных событий

События сортируются по timestamp с использованием ConcurrentSkipListMap для корректного порядка воспроизведения.

## 🔒 Потокобезопасность

- **ConcurrentHashMap** для хранения состояний
- **ConcurrentSkipListMap** для событий и снимков
- **ReadWriteLock** для атомарных операций над сущностями
- **AtomicBoolean/AtomicLong** для счетчиков и флагов

## 📈 Масштабирование

### Горизонтальное масштабирование

Система может быть расширена для работы с несколькими узлами через:
- Шардирование по entityId
- Распределенные снимки состояний
- Внешние системы очередей (Kafka, RabbitMQ)

### Вертикальное масштабирование

- Настройка размеров очередей
- Оптимизация частоты снимков
- Настройка пулов потоков

## 🤝 Расширение системы

### Новые типы событий

1. Реализуйте интерфейс `EventHandler`
2. Зарегистрируйте в `EventHandlerRegistry`
3. Система автоматически начнет обрабатывать новые события

### Новые источники событий

1. Реализуйте интерфейс `EventStream`
2. Подключите к `EventProcessor`

## 📝 Примеры использования

### Банковские операции

```java
// Создание счета
eventStream.addEvent("account-123", "ENTITY_CREATED", 
    Map.of("balance", 0.0, "status", "ACTIVE"));

// Депозит
eventStream.addEvent("account-123", "ACCOUNT_DEPOSIT", 
    Map.of("amount", 1000.0, "description", "Initial deposit"));

// Снятие
eventStream.addEvent("account-123", "ACCOUNT_WITHDRAW", 
    Map.of("amount", 200.0, "description", "ATM withdrawal"));

// Откат к состоянию до снятия
Instant beforeWithdraw = /* время до снятия */;
Optional<EntityState> previousState = 
    rollbackService.rollbackEntityState("account-123", beforeWithdraw);
```

### E-commerce заказы

```java
// Создание заказа
eventStream.addEvent("order-456", "ENTITY_CREATED", 
    Map.of("customerId", "user-123", "amount", 99.99, "status", "pending"));

// Оплата
eventStream.addEvent("order-456", "ORDER_PAID", 
    Map.of("paymentId", "pay-789", "method", "credit_card"));

// Отмена заказа (откат к предыдущему состоянию)
rollbackService.rollbackEntityState("order-456", beforePayment);
```

## 📄 Лицензия

Этот проект является демонстрационным и предназначен для образовательных целей.

## 👥 Авторы

Разработано как техническое задание для демонстрации архитектурных принципов высокопроизводительных систем обработки событий.

---

## 🔗 Связанные технологии

- Event Sourcing
- CQRS (Command Query Responsibility Segregation)  
- Stream Processing
- Snapshot Pattern
- Copy-on-Write
- Lock-Free Programming
