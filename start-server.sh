#!/bin/bash
PORT=${1:-9999}
THREADS=${2:-10}
CHANNEL=${3:-ASCII4}

echo "Starting IsoWire Server on port $PORT with $THREADS threads..."
echo "Press Ctrl+C to stop"

./mvnw -pl isowire-examples exec:java@run-server   -Dexec.args="$PORT $THREADS $CHANNEL"
