package ski.servlet.service;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class MessageProducer {
  private static final Logger logger = LoggerFactory.getLogger(MessageProducer.class);

  private final String host;
  private final int port;
  private final String username;
  private final String password;
  private final String queueName = "ski-rides";

  private Connection connection;
  private Channel channel;
  private final Gson gson = new Gson();

  private static MessageProducer instance;


  public MessageProducer(String host, int port, String username, String password) {
    this.host = host;
    this.port = port;
    this.username = username;
    this.password = password;
    init();
  }


  public static synchronized MessageProducer getInstance(String host, int port, String username, String password) {
    if (instance == null) {
      instance = new MessageProducer(host, port, username, password);
    } else if (!instance.isConnectionValid() ||
        !instance.host.equals(host) ||
        instance.port != port ||
        !instance.username.equals(username) ||
        !instance.password.equals(password)) {

      try {
        if (instance != null) {
          instance.cleanup();
        }
      } catch (Exception e) {
        logger.warn("Error cleaning up previous MessageProducer instance", e);
      }
      instance = new MessageProducer(host, port, username, password);
    }
    return instance;
  }

  private boolean isConnectionValid() {
    return connection != null && connection.isOpen() && channel != null && channel.isOpen();
  }

  private void init() {
    try {
      logger.info("Initializing RabbitMQ connection to {}:{} with username {}",
          host, port, username);

      ConnectionFactory factory = new ConnectionFactory();
      factory.setHost(host);
      factory.setPort(port);
      factory.setUsername(username);
      factory.setPassword(password);


      factory.setConnectionTimeout(5000);

      logger.info("Creating RabbitMQ connection...");
      this.connection = factory.newConnection();
      this.channel = connection.createChannel();

      logger.info("Declaring queue: {}", queueName);
      channel.queueDeclare(queueName, true, false, false, null);
      logger.info("RabbitMQ connection established successfully");
    } catch (Exception e) {
      logger.error("Failed to initialize RabbitMQ connection: {}", e.getMessage(), e);
    }
  }

  public void sendLiftRideEvent(LiftRideEvent event) throws IOException {
    try {

      if (!isConnectionValid()) {
        logger.warn("Connection to RabbitMQ is not valid. Attempting to reconnect...");
        init();


        if (!isConnectionValid()) {
          throw new IOException("Failed to establish valid connection to RabbitMQ after reconnect attempt");
        }
      }

      String jsonMessage = gson.toJson(event);
      logger.info("Sending message to queue {}: {}", queueName, jsonMessage);
      channel.basicPublish("", queueName, null, jsonMessage.getBytes());
      logger.info("Message sent successfully");
    } catch (Exception e) {
      logger.error("Error sending message to RabbitMQ: {}", e.getMessage(), e);


      try {
        logger.info("Attempting to reconnect to RabbitMQ...");
        init();


        if (isConnectionValid()) {
          String jsonMessage = gson.toJson(event);
          channel.basicPublish("", queueName, null, jsonMessage.getBytes());
          logger.info("Message sent successfully after reconnection");
        } else {
          throw new IOException("Failed to reestablish connection to RabbitMQ");
        }
      } catch (Exception reconnectEx) {
        logger.error("Failed to reconnect and send message: {}", reconnectEx.getMessage(), reconnectEx);
        throw new IOException("Failed to send message to RabbitMQ after reconnection", reconnectEx);
      }
    }
  }

  public void cleanup() {
    logger.info("Closing RabbitMQ connection");
    try {
      if (channel != null && channel.isOpen()) {
        channel.close();
      }
      if (connection != null && connection.isOpen()) {
        connection.close();
      }
    } catch (IOException | TimeoutException e) {
      logger.error("Error while closing RabbitMQ connection", e);
    }
  }
}