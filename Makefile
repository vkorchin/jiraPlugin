# Makefile для Event Stream Processor

# Директории
SRC_DIR = src/main/java
TARGET_DIR = target/classes
PACKAGE = com/eventstream

# Java файлы
JAVA_FILES = $(shell find $(SRC_DIR) -name "*.java")

# Основные цели
.PHONY: all clean compile run-example run-benchmark run-advanced

all: compile

# Компиляция
compile:
	@echo "🔨 Компиляция Event Stream Processor..."
	@mkdir -p $(TARGET_DIR)
	@javac -d $(TARGET_DIR) $(JAVA_FILES)
	@echo "✅ Компиляция завершена успешно!"

# Запуск примера
run-example: compile
	@echo "🎯 Запуск примера..."
	@java -cp $(TARGET_DIR) com.eventstream.Example

# Запуск бенчмарка
run-benchmark: compile
	@echo "🚀 Запуск бенчмарка производительности..."
	@java -cp $(TARGET_DIR) com.eventstream.utils.PerformanceBenchmark

# Запуск расширенного примера
run-advanced: compile
	@echo "🚀 Запуск расширенного примера..."
	@java -cp $(TARGET_DIR) com.eventstream.AdvancedExample

# Очистка
clean:
	@echo "🧹 Очистка..."
	@rm -rf $(TARGET_DIR)
	@echo "✅ Очистка завершена"

# Помощь
help:
	@echo "Доступные команды:"
	@echo "  make compile        - Компиляция проекта"
	@echo "  make run-example    - Запуск простого примера"
	@echo "  make run-advanced   - Запуск расширенного примера"
	@echo "  make run-benchmark  - Запуск бенчмарка производительности"
	@echo "  make clean          - Очистка скомпилированных файлов"
	@echo "  make help           - Показать эту справку"