package neu.cs6650.consumer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RabbitMQConsumerManager {
  private static final Logger logger = LoggerFactory.getLogger(RabbitMQConsumerManager.class);

  private final String host;
  private final int port;
  private final String username;
  private final String password;
  private final String queueName;
  private final int threadCount;
  private final DynamoDBService dynamoDBService;

  private Connection connection;
  private final List<RabbitMQConsumer> consumers = new ArrayList<>();
  private ExecutorService executorService;
  private ScheduledExecutorService monitorService;
  private final AtomicInteger activeThreads = new AtomicInteger(0);

  public RabbitMQConsumerManager(
      String host, int port, String username, String password,
      String queueName, int threadCount, DynamoDBService dynamoDBService) {
    this.host = host;
    this.port = port;
    this.username = username;
    this.password = password;
    this.queueName = queueName;
    this.threadCount = threadCount;
    this.dynamoDBService = dynamoDBService;
  }

  public void start() throws Exception {
    logger.info("Starting consumer manager with {} threads", threadCount);


    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(host);
    factory.setPort(port);
    factory.setUsername(username);
    factory.setPassword(password);

    this.connection = factory.newConnection();
    this.executorService = Executors.newFixedThreadPool(threadCount);


    for (int i = 0; i < threadCount; i++) {
      final int threadId = i;
      executorService.submit(() -> {
        try {

          Channel channel = connection.createChannel();


          RabbitMQConsumer consumer = new RabbitMQConsumer(
              channel, queueName, dynamoDBService);
          consumers.add(consumer);

          logger.info("Starting consumer thread {}", threadId);
          activeThreads.incrementAndGet();
          consumer.start();
        } catch (Exception e) {
          logger.error("Failed to start consumer thread {}", threadId, e);
        } finally {
          activeThreads.decrementAndGet();
        }
      });
    }


    startQueueMonitoring();
  }

  private void startQueueMonitoring() {
    monitorService = Executors.newScheduledThreadPool(1);
    monitorService.scheduleAtFixedRate(() -> {
      try {
        long queueSize = getQueueSize();
        logger.info("Current queue size: {}", queueSize);


        adjustThreadCount(queueSize);
      } catch (Exception e) {
        logger.error("Error in queue monitoring", e);
      }
    }, 30, 30, TimeUnit.SECONDS);
  }

  private long getQueueSize() throws Exception {
    try (Channel channel = connection.createChannel()) {
      return channel.messageCount(queueName);
    }
  }

  private void adjustThreadCount(long queueSize) {
    int currentThreadCount = activeThreads.get();

    if (queueSize > 1000 && currentThreadCount < 128) {

      int newThreadCount = Math.min(currentThreadCount + 16, 128);
      addMoreConsumerThreads(newThreadCount - currentThreadCount);
      logger.info("Increased consumer threads to: {}", newThreadCount);
    } else if (queueSize < 100 && currentThreadCount > 32) {

      logger.info("Reduced consumer threads target to: 32");
    }
  }

  private void addMoreConsumerThreads(int count) {
    for (int i = 0; i < count; i++) {
      executorService.submit(() -> {
        try {
          Channel channel = connection.createChannel();
          RabbitMQConsumer consumer = new RabbitMQConsumer(
              channel, queueName, dynamoDBService);
          consumers.add(consumer);
          activeThreads.incrementAndGet();
          consumer.start();
        } catch (IOException e) {
          logger.error("Failed to start additional consumer", e);
        } finally {
          activeThreads.decrementAndGet();
        }
      });
    }
  }

  public void shutdown() {
    logger.info("Shutting down consumer manager");

    if (monitorService != null) {
      monitorService.shutdown();
    }


    for (RabbitMQConsumer consumer : consumers) {
      try {
        consumer.close();
      } catch (Exception e) {
        logger.error("Error closing consumer", e);
      }
    }


    if (executorService != null) {
      executorService.shutdown();
      try {
        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
          executorService.shutdownNow();
        }
      } catch (InterruptedException e) {
        executorService.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }


    try {
      if (connection != null && connection.isOpen()) {
        connection.close();
      }
    } catch (IOException e) {
      logger.error("Error closing RabbitMQ connection", e);
    }

    logger.info("Consumer manager shutdown complete");
  }
}
