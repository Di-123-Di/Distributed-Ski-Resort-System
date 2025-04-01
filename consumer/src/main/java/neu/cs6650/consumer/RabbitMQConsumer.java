package neu.cs6650.consumer;

import com.google.gson.Gson;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RabbitMQConsumer {

  private static final Logger logger = LoggerFactory.getLogger(RabbitMQConsumer.class);

  private final Channel channel;
  private final String queueName;
  private final DynamoDBService dynamoDBService;
  private final Gson gson = new Gson();
  private String consumerTag;

  public RabbitMQConsumer(Channel channel, String queueName, DynamoDBService dynamoDBService)
      throws IOException {
    this.channel = channel;
    this.queueName = queueName;
    this.dynamoDBService = dynamoDBService;

    channel.queueDeclare(queueName, true, false, false, null);
    channel.basicQos(500);
  }

  public void start() throws IOException {
    logger.info("Starting to consume messages from queue: {}", queueName);

    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
      String message = new String(delivery.getBody(), StandardCharsets.UTF_8);

      try {
        LiftRideEvent event = gson.fromJson(message, LiftRideEvent.class);

        long startTime = System.currentTimeMillis();
        processMessage(event);
        long endTime = System.currentTimeMillis();

        if ((endTime - startTime) > 500) {
          logger.warn("Message processing took {} ms", (endTime - startTime));
        }

        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
      } catch (Exception e) {
        logger.error("Error processing message: {}", message, e);
        channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
      }
    };

    CancelCallback cancelCallback = consumerTag ->
        logger.warn("Consumer {} was cancelled", consumerTag);

    this.consumerTag = channel.basicConsume(queueName, false, deliverCallback, cancelCallback);
  }

  private void processMessage(LiftRideEvent event) {
    dynamoDBService.addLiftRide(
        event.getSkierID(),
        event.getLiftRide(),
        event.getResortID(),
        event.getSeasonID(),
        event.getDayID()
    );
  }

  public void close() throws IOException {
    if (channel != null && channel.isOpen()) {
      try {
        if (consumerTag != null) {
          channel.basicCancel(consumerTag);
        }
        channel.close();
      } catch (java.util.concurrent.TimeoutException e) {
        logger.error("Timeout while closing channel", e);
        throw new IOException("Timeout while closing channel", e);
      }
    }
  }
}