#!/bin/bash

RABBITMQ_HOST=${1:-$RABBITMQ_HOST}
RABBITMQ_USERNAME=${RABBITMQ_USERNAME:-"myuser"}
RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD:-"mypassword"}
RABBITMQ_PORT=${RABBITMQ_PORT:-5672}

if [ -z "$RABBITMQ_HOST" ]; then
  echo "Error: RABBITMQ_HOST not set. Please provide it as an argument: ./start-modified-server.sh <rabbitmq-host>"
  exit 1
fi

echo "Starting server with RabbitMQ host: $RABBITMQ_HOST"


pkill -f TomcatLauncher

mkdir -p extracted
rm -rf extracted/*
unzip server-servlet-executable.war -d extracted
cd extracted


nohup java -cp "WEB-INF/classes:WEB-INF/lib/*" \
  -Xmx512m \
  -Djava.net.preferIPv4Stack=true \
  -Dorg.apache.catalina.connector.RECYCLE_FACADES=false \
  -Dorg.apache.catalina.startup.EXIT_ON_INIT_FAILURE=true \
  ski.servlet.launcher.TomcatLauncher \
  --rabbitmq.host=$RABBITMQ_HOST \
  --rabbitmq.username=$RABBITMQ_USERNAME \
  --rabbitmq.password=$RABBITMQ_PASSWORD \
  --rabbitmq.port=$RABBITMQ_PORT > ../modified-server.log 2>&1 &

SERVER_PID=$!
echo "Server started with PID: $SERVER_PID"
echo "Waiting for server to start..."
sleep 10


if ps -p $SERVER_PID > /dev/null; then
  echo "Server process is still running"
else
  echo "Server process has died!"
  tail -n 50 ../modified-server.log
  exit 1
fi

echo "Checking if server is running on port 8081:"
sudo netstat -tulnp | grep 8081 || echo "No process found listening on port 8081"


echo "Last 20 lines of log:"
tail -n 20 ../modified-server.log