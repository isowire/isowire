#!/bin/bash
# Compile project using Maven wrapper
echo "Compiling project..."
./mvnw clean compile

if [ $? -eq 0 ]; then
    CLASS_COUNT=$(find target/classes -name "*.class" | wc -l)
    echo "✓ Successfully compiled $CLASS_COUNT classes"
else
    echo "✗ Compilation failed"
    exit 1
fi