# Event Stream Processor - Система потоковой обработки событий с откатом состояний

## 🎯 Описание

Система потоковой обработки событий на Java с поддержкой отката состояний сущностей на произвольную временную точку. Система спроектирована для высокой производительности и масштабируемости.

## ✨ Основные возможности

- **Потоковая обработка событий** - непрерывная обработка входящих событий в реальном времени
- **Откат состояний** - восстановление состояния любой сущности на произвольный момент времени
- **Система снимков** - автоматическое создание снимков состояний для оптимизации откатов
- **Потокобезопасность** - безопасная работа в многопоточной среде
- **Высокая производительность** - обработка миллионов событий в минуту
- **Быстрый откат** - целевое время отката < 100 мс
- **Расширяемость** - легко добавлять новые типы событий без изменения базового кода

## 🏗️ Архитектура

### Основные компоненты

1. **EventStreamProcessor** - главный процессор событий
2. **EventStore** - хранилище событий для каждой сущности
3. **SnapshotManager** - менеджер снимков состояний
4. **EventHandler** - интерфейс для обработки различных типов событий
5. **Entity** - представление состояния сущности

### Слои системы

```
┌─────────────────────────────────────────────────────────┐
│                    EventStreamProcessor                 │
├─────────────────────────────────────────────────────────┤
│  EventStore  │  SnapshotManager  │  EventHandlers     │
├─────────────────────────────────────────────────────────┤
│                    Concurrent Data Structures          │
└─────────────────────────────────────────────────────────┘
```

## 🚀 Быстрый старт

### Зависимости

Система использует только стандартные библиотеки Java:
- `java.util.concurrent` - многопоточность
- `java.time` - работа с временем
- `java.util.concurrent.locks` - блокировки
- `java.util.concurrent.atomic` - атомарные операции

### Компиляция

```bash
# Создаем директории для классов
mkdir -p target/classes

# Компилируем Java файлы
javac -d target/classes src/main/java/com/eventstream/**/*.java
```

### Запуск примера

```bash
# Запускаем простой пример
java -cp target/classes com.eventstream.Example

# Запускаем бенчмарк производительности
java -cp target/classes com.eventstream.utils.PerformanceBenchmark
```

## 📖 Примеры использования

### Базовое использование

```java
// Создаем процессор
EventStreamProcessor processor = new EventStreamProcessor(100, 4);

// Регистрируем обработчики
processor.registerEventHandler(new UserEventHandler());

// Создаем событие
Map<String, Object> payload = new HashMap<>();
payload.put("name", "Иван Иванов");
payload.put("email", "ivan@example.com");

Event event = processor.createEvent("user_123", Instant.now(), "USER_CREATED", payload);

// Обрабатываем событие
processor.processEvent(event).join();

// Получаем состояние на определенный момент времени
Instant targetTime = Instant.now().minusMinutes(30);
Entity stateAtTime = processor.getEntityStateAt("user_123", targetTime).join();
```

### Создание собственного обработчика событий

```java
public class OrderEventHandler implements EventHandler {
    
    @Override
    public boolean canHandle(String eventType) {
        return "ORDER_CREATED".equals(eventType) || 
               "ORDER_UPDATED".equals(eventType);
    }
    
    @Override
    public Entity handle(Event event, Entity currentEntity) {
        // Логика обработки события
        Entity updatedEntity = currentEntity.copy();
        updatedEntity.updateState(event.getPayload());
        return updatedEntity;
    }
    
    @Override
    public Entity rollback(Event event, Entity currentEntity) {
        // Логика отката события
        return currentEntity;
    }
}
```

## ⚡ Производительность

### Целевые показатели

- **Обработка событий**: > 1,000,000 событий/минуту
- **Время отката**: < 100 мс
- **Потребление памяти**: линейный рост с количеством активных сущностей
- **Масштабируемость**: линейное масштабирование с количеством потоков

### Оптимизации

1. **Система снимков** - создание снимков каждые N событий для быстрого отката
2. **Copy-on-write** - минимальное копирование данных при обновлениях
3. **Параллельные структуры данных** - использование `ConcurrentHashMap`, `ConcurrentSkipListMap`
4. **ReadWriteLock** - разделение блокировок чтения и записи для параллельных операций
5. **Асинхронная обработка** - использование `CompletableFuture` для неблокирующих операций

## 🔒 Потокобезопасность

### Механизмы защиты

- **ReadWriteLock** для каждой сущности - позволяет параллельное чтение
- **ConcurrentHashMap** для хранения состояний и событий
- **AtomicLong** для счетчиков и версий
- **Copy-on-write** подход для обновления состояний

### Обработка гонок состояний

```java
// Блокировка на запись при обработке события
entityLock.writeLock().lock();
try {
    // Обработка события
    Entity updatedEntity = processEventWithHandler(event, currentEntity);
    currentStates.put(entityId, updatedEntity);
} finally {
    entityLock.writeLock().unlock();
}

// Блокировка на чтение при откате
entityLock.readLock().lock();
try {
    return reconstructEntityState(entityId, timestamp);
} finally {
    entityLock.readLock().unlock();
}
```

## 🧹 Управление памятью

### Стратегии очистки

1. **Автоматическая очистка** - удаление старых событий и снимков
2. **Настраиваемые пороги** - конфигурируемые временные рамки для хранения данных
3. **Сжатие снимков** - возможность архивирования старых снимков

```java
// Очистка данных старше 30 дней
Instant cutoffTime = Instant.now().minusSeconds(30 * 24 * 60 * 60);
processor.cleanupOldData(cutoffTime);
```

## 🧪 Тестирование

### Бенчмарк производительности

Система включает встроенный бенчмарк для тестирования:

- Обработка событий
- Откат состояний
- Нагрузочное тестирование
- Измерение производительности

### Запуск тестов

```bash
java -cp target/classes com.eventstream.utils.PerformanceBenchmark
```

## 🔧 Конфигурация

### Основные параметры

```java
EventStreamProcessor processor = new EventStreamProcessor(
    snapshotFrequency,  // Частота создания снимков (каждые N событий)
    threadPoolSize      // Размер пула потоков для обработки
);
```

### Рекомендуемые настройки

- **snapshotFrequency**: 100-1000 (зависит от частоты событий)
- **threadPoolSize**: количество ядер CPU * 2
- **cleanup frequency**: каждые 24 часа для продакшена

## 🚨 Обработка ошибок

### Стратегии восстановления

1. **Логирование ошибок** - детальное логирование всех операций
2. **Graceful degradation** - продолжение работы при частичных сбоях
3. **Retry механизмы** - повторные попытки для временных ошибок
4. **Circuit breaker** - защита от каскадных сбоев

## 🔮 Расширение системы

### Добавление новых типов событий

1. Создать новый `EventHandler`
2. Зарегистрировать в `EventStreamProcessor`
3. Система автоматически начнет обрабатывать новые типы

### Интеграция с внешними системами

```java
// Пример интеграции с Kafka
@KafkaListener(topics = "events")
public void handleKafkaEvent(String eventJson) {
    Event event = parseEvent(eventJson);
    processor.processEvent(event);
}
```

## 📊 Мониторинг

### Метрики системы

- Количество активных сущностей
- Общее количество событий
- Количество снимков
- Время обработки событий
- Время отката состояний

```java
SystemStats stats = processor.getSystemStats();
System.out.println("Active entities: " + stats.getActiveEntities());
System.out.println("Total events: " + stats.getTotalEvents());
System.out.println("Total snapshots: " + stats.getTotalSnapshots());
```

## 🤝 Вклад в проект

1. Форкните репозиторий
2. Создайте ветку для новой функции
3. Внесите изменения
4. Добавьте тесты
5. Создайте Pull Request

## 📄 Лицензия

MIT License - см. файл LICENSE для деталей.

## 📞 Поддержка

- Создайте Issue для багов или предложений
- Обсудите в Discussions
- Свяжитесь с командой разработки

---

**Примечание**: Эта система предназначена для высоконагруженных приложений, требующих надежной обработки событий с возможностью отката состояний. Рекомендуется тщательное тестирование в вашей среде перед использованием в продакшене.
