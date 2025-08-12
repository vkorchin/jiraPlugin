#!/bin/bash

echo "=== Компиляция системы потоковой обработки событий ==="

# Создаем директории для классов
mkdir -p target/classes

# Компилируем все Java файлы
echo "Компилирование исходного кода..."
find src -name "*.java" -print0 | xargs -0 javac -d target/classes -cp target/classes

if [ $? -eq 0 ]; then
    echo "✓ Компиляция успешно завершена"
    
    echo ""
    echo "=== Доступные команды ==="
    echo "1. Запуск демонстрации:"
    echo "   java -cp target/classes com.eventprocessing.demo.EventProcessingDemo"
    echo ""
    echo "2. Запуск тестов:"
    echo "   java -ea -cp target/classes com.eventprocessing.EventProcessingTest"
    echo ""
    
    # Автоматически запускаем демонстрацию
    if [ "$1" = "demo" ]; then
        echo "Запуск демонстрации..."
        java -cp target/classes com.eventprocessing.demo.EventProcessingDemo
    elif [ "$1" = "test" ]; then
        echo "Запуск тестов..."
        java -ea -cp target/classes com.eventprocessing.EventProcessingTest
    fi
else
    echo "✗ Ошибка компиляции"
    exit 1
fi