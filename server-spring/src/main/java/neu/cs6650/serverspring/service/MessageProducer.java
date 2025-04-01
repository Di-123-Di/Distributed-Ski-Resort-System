package neu.cs6650.serverspring.service;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Service
public class MessageProducer {

  private static final Logger logger = LoggerFactory.getLogger(MessageProducer.class);

  @Value("${rabbitmq.host}")
  private String host;

  @Value("${rabbitmq.port:5672}")
  private int port;

  @Value("${rabbitmq.username:guest}")
  private String username;

  @Value("${rabbitmq.password:guest}")
  private String password;

  @Value("${rabbitmq.queue.ski-rides}")
  private String queueName;

  private Connection connection;
  private Channel channel;
  private final Gson gson = new Gson();

  @PostConstruct
  public void init() throws IOException, TimeoutException {
    logger.info("Initializing RabbitMQ connection to {}:{}", host, port);

    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(host);
    factory.setPort(port);
    factory.setUsername(username);
    factory.setPassword(password);


    this.connection = factory.newConnection();
    this.channel = connection.createChannel();


    channel.queueDeclare(queueName, true, false, false, null);
    logger.info("RabbitMQ connection established successfully");
  }

  public void sendLiftRideEvent(LiftRideEvent event) throws IOException {
    String jsonMessage = gson.toJson(event);
    channel.basicPublish("", queueName, null, jsonMessage.getBytes());
    logger.debug("Message sent to queue: {}", jsonMessage);
  }

  @PreDestroy
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
