# 🚀 Быстрый старт Event Stream Processor

## 📋 Требования

- **Java**: версия 11 или выше
- **Память**: минимум 512 MB RAM
- **ОС**: Linux, macOS, Windows

## ⚡ Мгновенный запуск

### 1. Компиляция проекта

```bash
# Используя Makefile (рекомендуется)
make compile

# Или вручную
mkdir -p target/classes
javac -d target/classes src/main/java/com/eventstream/**/*.java
```

### 2. Запуск простого примера

```bash
make run-example
```

### 3. Запуск расширенного примера

```bash
make run-advanced
```

### 4. Запуск бенчмарка производительности

```bash
make run-benchmark
```

## 🎯 Что вы увидите

### Простой пример
```
🎯 Пример использования EventStreamProcessor

1️⃣ Создание пользователя...
✅ Пользователь создан: {name=Иван Иванов, email=ivan@example.com, age=30, status=ACTIVE, createdAt=...}

2️⃣ Обновление пользователя...
✅ Пользователь обновлен: {name=Иван Иванов, email=ivan@example.com, age=31, city=Москва, status=ACTIVE, createdAt=...}

3️⃣ Еще одно обновление...
✅ Пользователь обновлен: {name=Иван Иванов, email=ivan@example.com, age=32, city=Москва, job=Разработчик, status=ACTIVE, createdAt=...}

4️⃣ Тестирование отката состояния...
⏪ Состояние откачено к первому обновлению: {name=Иван Иванов, email=ivan@example.com, age=31, city=Москва, status=ACTIVE, createdAt=...}
⏪ Состояние откачено к моменту создания: {name=Иван Иванов, email=ivan@example.com, age=30, status=ACTIVE, createdAt=...}

📊 Статистика системы:
SystemStats{activeEntities=1, totalEvents=3, totalSnapshots=1}

🏁 Пример завершен!
```

### Расширенный пример
```
🚀 Расширенный пример EventStreamProcessor

👥 Тестирование workflow пользователей...
✅ Пользователь создан и обновлен: {name=Анна Петрова, email=anna@example.com, age=26, city=Санкт-Петербург, job=Дизайнер, interests=Искусство, путешествия, status=ACTIVE, createdAt=...}

🛒 Тестирование workflow заказов...
✅ Заказ создан, обновлен и завершен: {customerId=user_advanced_001, items=Книга 'Java Concurrency in Practice', amount=2400.0, currency=RUB, discount=10%, notes=Заказ успешно доставлен, deliveryDate=..., status=COMPLETED, createdAt=...}

⚡ Тестирование параллельной обработки...
✅ Параллельно обработано 100 событий за 245 мс
⚡ Средняя производительность: 408.16 событий/сек

⏪ Тестирование отката состояний...
🔄 Тестирование откатов...
⏪ Состояние на момент создания: {name=Тест Откат, status=ACTIVE}
⏪ Промежуточное состояние: {name=Тест Откат, step=2, value=update_2, status=ACTIVE}
✅ Текущее состояние: {name=Тест Откат, step=5, value=update_5, status=ACTIVE}

📊 Финальная статистика:
SystemStats{activeEntities=103, totalEvents=108, totalSnapshots=22}

🏁 Расширенный пример завершен!
```

### Бенчмарк производительности
```
🚀 Запуск бенчмарка производительности...

📝 Тест обработки 10000 событий...
✅ Обработано 10000 событий за 1247 мс
⚡ Производительность: 8019.25 событий/сек

⏪ Тест отката 1000 состояний...
✅ Выполнено 1000 откатов за 89 мс
⚡ Производительность откатов: 11235.96 откатов/сек

🔥 Нагрузочное тестирование 100000 событий...
✅ Нагрузочный тест завершен: 100000 событий за 12456 мс
⚡ Общая производительность: 8028.26 событий/сек

📊 Финальная статистика:
SystemStats{activeEntities=1100, totalEvents=111000, totalSnapshots=1110}
```

## 🔧 Настройка параметров

### Изменение частоты снимков

```java
// Снимок каждые 50 событий (по умолчанию 100)
EventStreamProcessor processor = new EventStreamProcessor(50, 4);
```

### Изменение размера пула потоков

```java
// 8 потоков для обработки (по умолчанию 4)
EventStreamProcessor processor = new EventStreamProcessor(100, 8);
```

### Настройка очистки памяти

```java
// Очистка данных старше 7 дней
Instant cutoffTime = Instant.now().minusSeconds(7 * 24 * 60 * 60);
processor.cleanupOldData(cutoffTime);
```

## 🚨 Устранение неполадок

### Ошибка компиляции
```bash
# Проверьте версию Java
java -version

# Должна быть 11 или выше
openjdk version "11.0.12" 2021-07-20
```

### Ошибка "Class not found"
```bash
# Убедитесь, что компиляция прошла успешно
make clean
make compile
```

### Медленная работа
```bash
# Увеличьте размер пула потоков
# Уменьшите частоту создания снимков
# Проверьте доступную память
```

## 📚 Следующие шаги

1. **Изучите архитектуру** - прочитайте `ARCHITECTURE.md`
2. **Создайте свой обработчик** - изучите примеры в `src/main/java/com/eventstream/handlers/`
3. **Настройте производительность** - поэкспериментируйте с параметрами
4. **Интегрируйте в свой проект** - используйте как библиотеку

## 🤝 Получение помощи

- 📖 **Документация**: `README.md`, `ARCHITECTURE.md`
- 💻 **Примеры**: `Example.java`, `AdvancedExample.java`
- 🧪 **Тесты**: `PerformanceBenchmark.java`
- 🐛 **Проблемы**: создайте Issue в репозитории

---

**Готово!** 🎉 Теперь у вас есть работающая система потоковой обработки событий с откатом состояний.