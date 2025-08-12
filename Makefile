# Makefile для EventStreamProcessor

# Переменные
JAVA_SRC = src/main/java
BUILD_DIR = build
MAIN_CLASS = com.eventstream.Demo
EXAMPLE_CLASS = com.eventstream.Example
PERF_TEST_CLASS = com.eventstream.utils.PerformanceTest

# Компиляция
.PHONY: compile
compile:
	@echo "📦 Компиляция Java файлов..."
	@mkdir -p $(BUILD_DIR)
	@javac -d $(BUILD_DIR) -cp . $(JAVA_SRC)/com/eventstream/**/*.java
	@echo "✅ Компиляция успешна!"

# Запуск демонстрации
.PHONY: demo
demo: compile
	@echo "🎭 Запуск демонстрации..."
	@java -cp $(BUILD_DIR) $(MAIN_CLASS)

# Запуск простого примера
.PHONY: example
example: compile
	@echo "🎯 Запуск простого примера..."
	@java -cp $(BUILD_DIR) $(EXAMPLE_CLASS)

# Запуск тестов производительности
.PHONY: perf
perf: compile
	@echo "⚡ Запуск тестов производительности..."
	@java -cp $(BUILD_DIR) $(PERF_TEST_CLASS)

# Тест многопоточности
.PHONY: concurrency
concurrency: compile
	@echo "🧵 Запуск теста многопоточности..."
	@java -cp $(BUILD_DIR) $(PERF_TEST_CLASS)

# Очистка
.PHONY: clean
clean:
	@echo "🧹 Очистка..."
	@rm -rf $(BUILD_DIR)
	@echo "✅ Очистка завершена"

# Показать структуру проекта
.PHONY: structure
structure:
	@echo "📁 Структура проекта:"
	@find $(JAVA_SRC) -name "*.java" | sort

# Показать скомпилированные классы
.PHONY: classes
classes: compile
	@echo "📦 Скомпилированные классы:"
	@find $(BUILD_DIR) -name "*.class" | sort

# Полная сборка и тест
.PHONY: all
all: clean compile demo

# Помощь
.PHONY: help
help:
	@echo "🚀 EventStreamProcessor - Доступные команды:"
	@echo ""
	@echo "  compile     - Компиляция проекта"
	@echo "  demo        - Запуск демонстрации"
	@echo "  example     - Запуск простого примера"
	@echo "  perf        - Запуск тестов производительности"
	@echo "  concurrency - Тест многопоточности"
	@echo "  clean       - Очистка скомпилированных файлов"
	@echo "  structure   - Показать структуру проекта"
	@echo "  classes     - Показать скомпилированные классы"
	@echo "  all         - Полная сборка и демонстрация"
	@echo "  help        - Показать эту справку"
	@echo ""
	@echo "Примеры использования:"
	@echo "  make demo        # Собрать и запустить демонстрацию"
	@echo "  make example     # Собрать и запустить простой пример"
	@echo "  make perf        # Собрать и запустить тесты производительности"
	@echo "  make clean       # Очистить проект"