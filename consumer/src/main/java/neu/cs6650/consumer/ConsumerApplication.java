package neu.cs6650.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerApplication {
  private static final Logger logger = LoggerFactory.getLogger(ConsumerApplication.class);

  public static void main(String[] args) {
    try {

      ConsumerConfig config = new ConsumerConfig(args);


      DynamoDBService dynamoDBService = new DynamoDBService();


      PerformanceMonitor monitor = new PerformanceMonitor(dynamoDBService);
      monitor.startMonitoring(10);


      RabbitMQConsumerManager consumerManager = new RabbitMQConsumerManager(
          config.getHost(),
          config.getPort(),
          config.getUsername(),
          config.getPassword(),
          config.getQueueName(),
          config.getThreadCount(),
          dynamoDBService
      );
      consumerManager.start();


      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        logger.info("Shutting down consumer application...");
        monitor.stopMonitoring();
        consumerManager.shutdown();
        dynamoDBService.shutdown();

        logger.info("Final statistics:");
        logger.info("Total processed messages: {}", dynamoDBService.getTotalProcessedMessages());
        logger.info("Unique skiers: {}", dynamoDBService.getUniqueSkiersCount());
        logger.info("Consumer application stopped");
      }));

    } catch (Exception e) {
      logger.error("Failed to start consumer application", e);
      System.exit(1);
    }
  }
}