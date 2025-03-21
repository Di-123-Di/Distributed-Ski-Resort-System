package neu.cs6650.serverspring.service;

import com.google.gson.Gson;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MessageProducer {

  @Autowired
  private RabbitTemplate rabbitTemplate;

  @Value("${rabbitmq.queue.ski-rides}")
  private String skiRidesQueue;

  private final Gson gson = new Gson();

  public void sendLiftRideEvent(LiftRideEvent event) {

    String jsonMessage = gson.toJson(event);
    rabbitTemplate.convertAndSend(skiRidesQueue, jsonMessage);
  }
}
