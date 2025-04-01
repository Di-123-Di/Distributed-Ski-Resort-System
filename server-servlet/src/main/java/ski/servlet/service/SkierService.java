package ski.servlet.service;

import io.swagger.client.model.LiftRide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkierService {
  private static final Logger logger = LoggerFactory.getLogger(SkierService.class);

  private final MessageProducer messageProducer;

  public SkierService(MessageProducer messageProducer) {
    this.messageProducer = messageProducer;
  }

  public void processLiftRide(LiftRide liftRide, int resortID, String seasonID, String dayID, int skierID) throws Exception {
    logger.info("Processing lift ride: skierID={}, resortID={}, liftID={}",
        skierID, resortID, liftRide.getLiftID());

    LiftRideEvent event = new LiftRideEvent(liftRide, resortID, seasonID, dayID, skierID);
    messageProducer.sendLiftRideEvent(event);

    logger.info("Lift ride event sent to queue successfully");
  }
}