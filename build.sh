#!/bin/bash

echo "🔨 Компиляция Event Stream Processor..."

# Создаем директории для классов
mkdir -p target/classes

# Компилируем Java файлы
echo "📝 Компиляция Java файлов..."
javac -d target/classes src/main/java/com/eventstream/**/*.java

if [ $? -eq 0 ]; then
    echo "✅ Компиляция завершена успешно!"
    
    echo ""
    echo "🚀 Доступные команды:"
    echo "  java -cp target/classes com.eventstream.Example                    # Простой пример"
    echo "  java -cp target/classes com.eventstream.utils.PerformanceBenchmark # Бенчмарк производительности"
    echo ""
    
    # Спрашиваем, что запустить
    read -p "Запустить простой пример? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "🎯 Запуск примера..."
        java -cp target/classes com.eventstream.Example
    fi
    
else
    echo "❌ Ошибка компиляции!"
    exit 1
fi