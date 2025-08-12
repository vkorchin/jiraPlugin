# 🔨 Компиляция и запуск Event Stream Processor

## 📋 Предварительные требования

### Проверка Java
```bash
java -version
```
**Требуется**: Java 11 или выше

### Проверка Make
```bash
make --version
```
**Требуется**: GNU Make 3.81 или выше

## 🚀 Быстрая компиляция

### Вариант 1: Использование Makefile (рекомендуется)
```bash
# Компиляция проекта
make compile

# Запуск простого примера
make run-example

# Запуск расширенного примера
make run-advanced

# Запуск бенчмарка производительности
make run-benchmark

# Очистка скомпилированных файлов
make clean

# Показать справку
make help
```

### Вариант 2: Ручная компиляция
```bash
# Создание директории для классов
mkdir -p target/classes

# Компиляция всех Java файлов
javac -d target/classes src/main/java/com/eventstream/**/*.java

# Запуск простого примера
java -cp target/classes com.eventstream.Example

# Запуск расширенного примера
java -cp target/classes com.eventstream.AdvancedExample

# Запуск бенчмарка
java -cp target/classes com.eventstream.utils.PerformanceBenchmark
```

## 🔍 Пошаговая компиляция

### Шаг 1: Проверка структуры проекта
```bash
# Убедитесь, что у вас есть следующая структура:
ls -la src/main/java/com/eventstream/
# Должны быть директории: core, handlers, storage, utils
```

### Шаг 2: Создание директории для классов
```bash
mkdir -p target/classes
```

### Шаг 3: Компиляция основных классов
```bash
# Компиляция core пакета
javac -d target/classes src/main/java/com/eventstream/core/*.java

# Компиляция handlers пакета
javac -d target/classes src/main/java/com/eventstream/handlers/*.java

# Компиляция storage пакета
javac -d target/classes src/main/java/com/eventstream/storage/*.java

# Компиляция utils пакета
javac -d target/classes src/main/java/com/eventstream/utils/*.java

# Компиляция примеров
javac -d target/classes src/main/java/com/eventstream/*.java
```

### Шаг 4: Проверка компиляции
```bash
# Проверка, что все классы скомпилированы
ls -la target/classes/com/eventstream/
```

## 🎯 Запуск примеров

### Простой пример
```bash
java -cp target/classes com.eventstream.Example
```

**Ожидаемый вывод:**
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
```bash
java -cp target/classes com.eventstream.AdvancedExample
```

**Ожидаемый вывод:**
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
```bash
java -cp target/classes com.eventstream.utils.PerformanceBenchmark
```

**Ожидаемый вывод:**
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

## 🚨 Устранение неполадок

### Ошибка: "javac: command not found"
```bash
# Установка Java Development Kit (Ubuntu/Debian)
sudo apt update
sudo apt install openjdk-11-jdk

# Установка Java Development Kit (CentOS/RHEL)
sudo yum install java-11-openjdk-devel

# Установка Java Development Kit (macOS)
brew install openjdk@11
```

### Ошибка: "make: command not found"
```bash
# Установка Make (Ubuntu/Debian)
sudo apt install make

# Установка Make (CentOS/RHEL)
sudo yum install make

# Установка Make (macOS)
# Обычно уже установлен
```

### Ошибка компиляции: "package does not exist"
```bash
# Убедитесь, что вы находитесь в корневой директории проекта
pwd
# Должно быть: /path/to/event-stream-processor

# Проверьте структуру директорий
ls -la src/main/java/com/eventstream/
```

### Ошибка: "Class not found"
```bash
# Очистите и перекомпилируйте
make clean
make compile

# Или вручную
rm -rf target/classes
mkdir -p target/classes
javac -d target/classes src/main/java/com/eventstream/**/*.java
```

### Медленная компиляция
```bash
# Используйте параллельную компиляцию
javac -d target/classes -J-Xmx2g src/main/java/com/eventstream/**/*.java

# Или увеличьте память для JVM
export JAVA_OPTS="-Xmx2g -Xms1g"
```

## 🔧 Настройка производительности

### Увеличение размера пула потоков
```bash
# В коде измените параметр threadPoolSize
EventStreamProcessor processor = new EventStreamProcessor(100, 8); // 8 потоков вместо 4
```

### Уменьшение частоты создания снимков
```bash
# В коде измените параметр snapshotFrequency
EventStreamProcessor processor = new EventStreamProcessor(50, 4); // снимок каждые 50 событий вместо 100
```

### Настройка JVM параметров
```bash
# Увеличение heap памяти
java -Xmx4g -Xms2g -cp target/classes com.eventstream.utils.PerformanceBenchmark

# Включение оптимизаций
java -XX:+UseG1GC -XX:+UseStringDeduplication -cp target/classes com.eventstream.utils.PerformanceBenchmark
```

## 📊 Проверка производительности

### Тест скорости компиляции
```bash
time make compile
```

### Тест скорости запуска
```bash
time java -cp target/classes com.eventstream.Example
```

### Тест производительности системы
```bash
# Запуск бенчмарка с измерением времени
time java -cp target/classes com.eventstream.utils.PerformanceBenchmark
```

## 🎉 Успешная компиляция

Если все прошло успешно, вы увидите:

✅ **Компиляция завершена** - все Java файлы скомпилированы  
✅ **Примеры запускаются** - система работает корректно  
✅ **Производительность** - обработка тысяч событий в секунду  
✅ **Откат состояний** - быстрое восстановление состояний  

**Поздравляем!** 🎉 Ваша система Event Stream Processor готова к использованию.

## 📚 Следующие шаги

1. **Изучите архитектуру** - прочитайте `ARCHITECTURE.md`
2. **Создайте свой обработчик** - изучите примеры в `src/main/java/com/eventstream/handlers/`
3. **Настройте параметры** - поэкспериментируйте с настройками производительности
4. **Интегрируйте в проект** - используйте как библиотеку в своем приложении

---

**Удачной компиляции!** 🚀