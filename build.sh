#!/bin/bash

echo "🚀 EventStreamProcessor - Сборка и запуск"
echo "=========================================="

# Создаем директорию для скомпилированных классов
mkdir -p build

# Компилируем все Java файлы
echo "📦 Компиляция Java файлов..."
javac -d build -cp . src/main/java/com/eventstream/**/*.java

if [ $? -eq 0 ]; then
    echo "✅ Компиляция успешна!"
    echo ""
    
    # Показываем меню
    echo "Выберите действие:"
    echo "1) Запустить демонстрацию"
    echo "2) Запустить тесты производительности"
    echo "3) Запустить тест многопоточности"
    echo "4) Показать структуру проекта"
    echo "5) Выход"
    echo ""
    read -p "Введите номер (1-5): " choice
    
    case $choice in
        1)
            echo "🎭 Запуск демонстрации..."
            java -cp build com.eventstream.Demo
            ;;
        2)
            echo "⚡ Запуск тестов производительности..."
            java -cp build com.eventstream.utils.PerformanceTest
            ;;
        3)
            echo "🧵 Запуск теста многопоточности..."
            java -cp build com.eventstream.utils.PerformanceTest
            ;;
        4)
            echo "📁 Структура проекта:"
            find src/main/java -name "*.java" | sort
            echo ""
            echo "📦 Скомпилированные классы:"
            find build -name "*.class" | sort
            ;;
        5)
            echo "👋 До свидания!"
            exit 0
            ;;
        *)
            echo "❌ Неверный выбор"
            exit 1
            ;;
    esac
else
    echo "❌ Ошибка компиляции!"
    exit 1
fi