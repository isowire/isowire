#!/bin/bash
HOST=${1:-localhost}
PORT=${2:-9999}
CHANNEL=${3:-ASCII4}

echo "Running IsoWire Test Client against $HOST:$PORT with channel $CHANNEL..."

./mvnw -pl isowire-examples exec:java@run-client -Dexec.args="$HOST $PORT $CHANNEL"
