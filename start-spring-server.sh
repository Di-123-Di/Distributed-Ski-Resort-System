#!/bin/bash

pkill -f "server-spring"


nohup java -jar server-spring-1.0-SNAPSHOT.jar \
  --server.port=8081 \
  --spring.rabbitmq.host=$1 \
  --spring.rabbitmq.username=myuser \
  --spring.rabbitmq.password=mypassword \
  --rabbitmq.host=$1 > spring-server.log 2>&1 &

echo "Spring Boot server started, PID: $!"