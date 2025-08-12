# 🚀 Быстрый старт EventStreamProcessor

## Что это?

EventStreamProcessor - это высокопроизводительная система на Java для обработки потоков событий с возможностью отката состояний на любую временную точку.

## Требования

- **Java**: 11 или выше
- **Память**: минимум 512MB RAM
- **ОС**: Linux, macOS, Windows

## Установка и запуск

### 1. Клонирование проекта

```bash
git clone <repository-url>
cd EventStreamProcessor
```

### 2. Быстрый запуск

#### Вариант A: Использование Makefile (рекомендуется)

```bash
# Показать доступные команды
make help

# Запустить простой пример
make example

# Запустить полную демонстрацию
make demo

# Запустить тесты производительности
make perf

# Очистить проект
make clean
```

#### Вариант B: Использование скрипта

```bash
# Сделать скрипт исполняемым
chmod +x build.sh

# Запустить интерактивное меню
./build.sh
```

#### Вариант C: Ручная компиляция

```bash
# Создать директорию для сборки
mkdir -p build

# Скомпилировать проект
javac -d build -cp . src/main/java/com/eventstream/**/*.java

# Запустить простой пример
java -cp build com.eventstream.Example

# Запустить демонстрацию
java -cp build com.eventstream.Demo

# Запустить тесты производительности
java -cp build com.eventstream.utils.PerformanceTest
```

## Первые шаги

### 1. Простой пример

```java
// Создаем процессор
EventStreamProcessor processor = new EventStreamProcessor(100, 10);

// Регистрируем обработчики
processor.registerEventProcessor(new UserEventProcessor());

// Создаем событие
Event event = new Event(
    "user-001",                    // ID сущности
    Instant.now(),                 // Временная метка
    "user.created",                // Тип события
    Map.of("name", "John Doe"),    // Данные события
    0                              // Номер последовательности
);

// Обрабатываем событие
processor.processEvent(event);

// Получаем текущее состояние
Entity state = processor.getCurrentEntityState("user-001");

// Откатываемся на определенное время
Instant pastTime = Instant.now().minusHours(1);
Entity pastState = processor.getEntityStateAt("user-001", pastTime);
```

### 2. Подключение к потоку событий

```java
// Создаем поток событий (1000 событий/сек)
SimulatedEventStream eventStream = new SimulatedEventStream(1000);

// Подписываемся на события
eventStream.subscribe(processor::processEvent);

// Запускаем поток
eventStream.start();

// ... работаем ...

// Останавливаем поток
eventStream.stop();
```

### 3. Мониторинг производительности

```java
// Создаем мониторинг
PerformanceMonitor monitor = new PerformanceMonitor(processor);

// Отслеживаем операции
monitor.startMetric("operation");
// ... выполняем операцию ...
monitor.endMetric("operation");

// Показываем статистику
monitor.printStats();
```

## Примеры использования

### Обработка пользовательских событий

```java
// Создаем процессор
EventStreamProcessor processor = new EventStreamProcessor();

// Регистрируем специализированный обработчик
processor.registerEventProcessor(new UserEventProcessor());

// Обрабатываем события пользователя
Event userCreated = new Event(
    "user-123",
    Instant.now().minusMinutes(10),
    "user.created",
    Map.of("name", "Alice", "email", "alice@example.com"),
    0
);

Event userUpdated = new Event(
    "user-123",
    Instant.now().minusMinutes(5),
    "user.updated",
    Map.of("age", 25, "city", "New York"),
    0
);

processor.processEvent(userCreated);
processor.processEvent(userUpdated);

// Получаем состояние на разные моменты времени
Instant now = Instant.now();
Entity currentState = processor.getCurrentEntityState("user-123");
Entity state10MinAgo = processor.getEntityStateAt("user-123", now.minusMinutes(10));
Entity state5MinAgo = processor.getEntityStateAt("user-123", now.minusMinutes(5));
```

### Создание собственного обработчика

```java
public class OrderEventProcessor implements EventProcessor {
    
    @Override
    public boolean canHandle(String eventType) {
        return eventType.startsWith("order.");
    }
    
    @Override
    public void handle(Event event, Entity entity) {
        switch (event.getEventType()) {
            case "order.created":
                entity.updateState("status", "created");
                entity.updateState("createdAt", event.getTimestamp());
                break;
            case "order.paid":
                entity.updateState("status", "paid");
                entity.updateState("paidAt", event.getTimestamp());
                break;
            case "order.shipped":
                entity.updateState("status", "shipped");
                entity.updateState("shippedAt", event.getTimestamp());
                break;
        }
    }
    
    @Override
    public int getPriority() {
        return 30; // Высокий приоритет
    }
}

// Регистрация
processor.registerEventProcessor(new OrderEventProcessor());
```

## Настройка производительности

### Оптимизация снимков

```java
// Снимок каждые 50 событий, максимум 20 снимков на сущность
EventStreamProcessor processor = new EventStreamProcessor(50, 20);

// Для высоконагруженных систем
EventStreamProcessor highPerfProcessor = new EventStreamProcessor(25, 50);
```

### Настройка пула потоков

```java
EventStreamProcessor processor = new EventStreamProcessor() {
    {
        // Переопределяем пул потоков
        this.eventProcessingExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2
        );
    }
};
```

## Мониторинг и диагностика

### Получение статистики

```java
// Статистика процессора
String processorStats = processor.getStatistics();
System.out.println(processorStats);

// Статистика хранилища событий
String eventStoreStats = processor.getEventStore().getStatistics();
System.out.println(eventStoreStats);

// Статистика снимков
String snapshotStats = processor.getSnapshotManager().getStatistics();
System.out.println(snapshotStats);
```

### Очистка старых данных

```java
// Очистка данных старше 7 дней
Instant cutoffTime = Instant.now().minusDays(7);
processor.cleanupOldData(cutoffTime);
```

## Тестирование

### Тест производительности

```java
PerformanceTest test = new PerformanceTest();

// Полный тест производительности
test.runPerformanceTest();

// Тест многопоточности
test.testConcurrency();
```

### Нагрузочное тестирование

```java
// Создаем высоконагруженный поток
SimulatedEventStream highLoadStream = new SimulatedEventStream(5000); // 5000 событий/сек

// Подписываемся на события
highLoadStream.subscribe(processor::processEvent);

// Запускаем нагрузку
highLoadStream.start();

// Даем время на тест
Thread.sleep(30000); // 30 секунд

// Останавливаем
highLoadStream.stop();
```

## Устранение неполадок

### Проблемы с памятью

```java
// Уменьшаем частоту снимков
EventStreamProcessor processor = new EventStreamProcessor(200, 5);

// Регулярно очищаем старые данные
ScheduledExecutorService cleanupExecutor = Executors.newScheduledThreadPool(1);
cleanupExecutor.scheduleAtFixedRate(() -> {
    Instant cutoffTime = Instant.now().minusDays(1);
    processor.cleanupOldData(cutoffTime);
}, 1, 1, TimeUnit.HOURS);
```

### Медленные откаты

```java
// Увеличиваем частоту снимков
EventStreamProcessor processor = new EventStreamProcessor(25, 30);

// Используем адаптивные стратегии отката
// (планируется в будущих версиях)
```

### Проблемы с многопоточностью

```java
// Проверяем размер пула потоков
int threadCount = Runtime.getRuntime().availableProcessors();
System.out.println("Доступно ядер: " + threadCount);

// Настраиваем размер пула
EventStreamProcessor processor = new EventStreamProcessor() {
    {
        this.eventProcessingExecutor = Executors.newFixedThreadPool(threadCount);
    }
};
```

## Следующие шаги

1. **Изучите архитектуру**: прочитайте `ARCHITECTURE.md`
2. **Посмотрите примеры**: изучите `Demo.java` и `Example.java`
3. **Запустите тесты**: используйте `PerformanceTest.java`
4. **Создайте свой обработчик**: реализуйте `EventProcessor`
5. **Интегрируйте в проект**: используйте как библиотеку

## Получение помощи

- **Документация**: `README.md`, `ARCHITECTURE.md`
- **Примеры**: `src/main/java/com/eventstream/`
- **Тесты**: `src/main/java/com/eventstream/utils/`

## Лицензия

MIT License - свободное использование для любых целей.

---

🎉 **Готово!** Теперь у вас есть работающая система потоковой обработки событий с откатами состояний.