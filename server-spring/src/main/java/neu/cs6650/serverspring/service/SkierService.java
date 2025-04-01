package neu.cs6650.serverspring.service;

import io.swagger.client.model.LiftRide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SkierService {

  private static final Logger logger = LoggerFactory.getLogger(SkierService.class);

  @Autowired
  private MessageProducer messageProducer;

  public void processLiftRide(LiftRide liftRide, int resortID, String seasonID, String dayID, int skierID) throws Exception {
    logger.info("Processing lift ride: skierID={}, resortID={}, liftID={}",
        skierID, resortID, liftRide.getLiftID());

    LiftRideEvent event = new LiftRideEvent(liftRide, resortID, seasonID, dayID, skierID);
    messageProducer.sendLiftRideEvent(event);

    logger.info("Lift ride event sent to queue successfully");
  }
}
