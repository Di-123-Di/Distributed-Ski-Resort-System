package neu.cs6650.consumer;

import com.google.gson.Gson;
import com.rabbitmq.client.*;
import io.swagger.client.model.LiftRide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;


public class RabbitMQConsumer {
  private static final Logger logger = LoggerFactory.getLogger(RabbitMQConsumer.class);

  private final String host;
  private final int port;
  private final String username;
  private final String password;
  private final String queueName;
  private final SkierDataStore dataStore;
  private final Gson gson = new Gson();

  private Connection connection;
  private Channel channel;

  public RabbitMQConsumer(String host, int port, String username, String password,
      String queueName, SkierDataStore dataStore) throws IOException {
    this.host = host;
    this.port = port;
    this.username = username;
    this.password = password;
    this.queueName = queueName;
    this.dataStore = dataStore;


    initializeConnection();
  }


  private void initializeConnection() throws IOException {
    try {

      ConnectionFactory factory = new ConnectionFactory();
      factory.setHost(host);
      factory.setPort(port);
      factory.setUsername(username);
      factory.setPassword(password);


      connection = factory.newConnection();
      channel = connection.createChannel();


      channel.queueDeclare(queueName, true, false, false, null);


      channel.basicQos(100);

      logger.info("Successfully connected to RabbitMQ at {}:{}", host, port);
    } catch (IOException | TimeoutException e) {
      logger.error("Failed to connect to RabbitMQ", e);
      throw new IOException("Failed to connect to RabbitMQ", e);
    }
  }


  public void start() throws IOException {
    logger.info("Starting to consume messages from queue: {}", queueName);


    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
      String message = new String(delivery.getBody(), StandardCharsets.UTF_8);

      try {

        LiftRideEvent event = gson.fromJson(message, LiftRideEvent.class);


        processMessage(event);


        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
      } catch (Exception e) {
        logger.error("Error processing message: {}", message, e);

        channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
      }
    };

    CancelCallback cancelCallback = consumerTag ->
        logger.warn("Consumer {} was cancelled", consumerTag);

    channel.basicConsume(queueName, false, deliverCallback, cancelCallback);
  }


  private void processMessage(LiftRideEvent event) {

    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }


    dataStore.addLiftRide(event.getSkierID(), event.getLiftRide());
  }


  public void close() {
    try {
      if (channel != null && channel.isOpen()) {
        channel.close();
      }
      if (connection != null && connection.isOpen()) {
        connection.close();
      }
      logger.info("RabbitMQ connection closed");
    } catch (IOException | TimeoutException e) {
      logger.error("Error closing RabbitMQ connection", e);
    }
  }
}