package neu.cs6650.consumer;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ConsumerApplication {
  private static final Logger logger = LoggerFactory.getLogger(ConsumerApplication.class);


  private static String rabbitMQHost = "52.34.129.119";
  private static int rabbitMQPort = 5672;
  private static String rabbitMQUsername = "myuser";
  private static String rabbitMQPassword = "mypassword";
  private static String queueName = "ski-rides";
  private static int consumerThreads = 64;
  private static SkierDataStore dataStore;

  public static void main(String[] args) {

    processArgs(args);

    logger.info("Starting consumer application with {} threads", consumerThreads);
    logger.info("Connecting to RabbitMQ at {}:{} with queue: {}",
        rabbitMQHost, rabbitMQPort, queueName);


    dataStore = new SkierDataStore();


    PerformanceMonitor monitor = new PerformanceMonitor(dataStore);
    monitor.startMonitoring(10);


    ExecutorService executorService = Executors.newFixedThreadPool(consumerThreads);


    for (int i = 0; i < consumerThreads; i++) {
      final int threadId = i;
      executorService.submit(() -> {
        try {
          RabbitMQConsumer consumer = new RabbitMQConsumer(
              rabbitMQHost,
              rabbitMQPort,
              rabbitMQUsername,
              rabbitMQPassword,
              queueName,
              dataStore);

          logger.info("Starting consumer thread {}", threadId);
          consumer.start();
        } catch (IOException e) {
          logger.error("Failed to start consumer thread {}", threadId, e);
        }
      });
    }


    monitorQueueAndAdjustThreads(dataStore, executorService);


    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.info("Shutting down consumer application...");
      monitor.stopMonitoring();
      executorService.shutdown();


      logger.info("Final statistics:");
      logger.info("Total processed messages: {}", dataStore.getTotalProcessedMessages());
      logger.info("Unique skiers: {}", dataStore.getUniqueSkiersCount());

      logger.info("Consumer application stopped");
    }));
  }

  private static void processArgs(String[] args) {
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--host":
          if (i + 1 < args.length) {
            rabbitMQHost = args[++i];
          }
          break;
        case "--port":
          if (i + 1 < args.length) {
            rabbitMQPort = Integer.parseInt(args[++i]);
          }
          break;
        case "--username":
          if (i + 1 < args.length) {
            rabbitMQUsername = args[++i];
          }
          break;
        case "--password":
          if (i + 1 < args.length) {
            rabbitMQPassword = args[++i];
          }
          break;
        case "--queue":
          if (i + 1 < args.length) {
            queueName = args[++i];
          }
          break;
        case "--threads":
          if (i + 1 < args.length) {
            consumerThreads = Integer.parseInt(args[++i]);
          }
          break;
        default:
          logger.warn("Unknown argument: {}", args[i]);
      }
    }
  }

  private static void monitorQueueAndAdjustThreads(SkierDataStore dataStore, ExecutorService executorService) {
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    scheduler.scheduleAtFixedRate(() -> {

      long queueSize = getCurrentQueueSize();
      logger.info("Current queue size: {}", queueSize);

      if (queueSize > 1000 && consumerThreads < 128) {

        int newThreadCount = Math.min(consumerThreads + 16, 128);
        addMoreConsumerThreads(executorService, newThreadCount - consumerThreads);
        consumerThreads = newThreadCount;
        logger.info("Increased consumer threads to: {}", consumerThreads);
      } else if (queueSize < 100 && consumerThreads > 32) {

        consumerThreads = Math.max(consumerThreads - 16, 32);
        logger.info("Reduced consumer threads to: {}", consumerThreads);
      }
    }, 30, 30, TimeUnit.SECONDS);
  }


  private static void addMoreConsumerThreads(ExecutorService executorService, int count) {
    for (int i = 0; i < count; i++) {
      executorService.submit(() -> {
        try {
          RabbitMQConsumer consumer = new RabbitMQConsumer(
              rabbitMQHost,
              rabbitMQPort,
              rabbitMQUsername,
              rabbitMQPassword,
              queueName,
              dataStore);
          consumer.start();
        } catch (IOException e) {
          logger.error("Failed to start additional consumer", e);
        }
      });
    }
  }


  private static long getCurrentQueueSize() {
    try {
      ConnectionFactory factory = new ConnectionFactory();
      factory.setHost(rabbitMQHost);
      factory.setPort(rabbitMQPort);
      factory.setUsername(rabbitMQUsername);
      factory.setPassword(rabbitMQPassword);

      try (Connection conn = factory.newConnection();
          Channel channel = conn.createChannel()) {

        AMQP.Queue.DeclareOk queueInfo = channel.queueDeclarePassive(queueName);
        return queueInfo.getMessageCount();
      }
    } catch (Exception e) {
      logger.error("Error getting queue size", e);
      return 0;
    }
  }
}
