package neu.cs6650.serverspring.controller;

import io.swagger.client.model.LiftRide;
import io.swagger.client.model.ResponseMsg;
import neu.cs6650.serverspring.service.LiftRideEvent;
import neu.cs6650.serverspring.service.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/skiers")
public class SkierController {

  private static final Logger logger = LoggerFactory.getLogger(SkierController.class);

  @Autowired
  private MessageProducer messageProducer;

  @PostMapping("/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}")
  public ResponseEntity<ResponseMsg> writeNewLiftRide(
      @PathVariable int resortID,
      @PathVariable String seasonID,
      @PathVariable String dayID,
      @PathVariable int skierID,
      @RequestBody LiftRide liftRide) {


    if (!isValidUrlParameters(resortID, seasonID, dayID, skierID)) {
      logger.warn("Invalid URL parameters: resortID={}, seasonID={}, dayID={}, skierID={}",
          resortID, seasonID, dayID, skierID);
      ResponseMsg response = new ResponseMsg();
      response.setMessage("Invalid URL path");
      return ResponseEntity
          .status(HttpStatus.BAD_REQUEST)
          .body(response);
    }


    if (!isValidLiftRide(liftRide)) {
      logger.warn("Invalid lift ride: liftID={}, time={}",
          liftRide.getLiftID(), liftRide.getTime());
      ResponseMsg response = new ResponseMsg();
      response.setMessage("Invalid request body");
      return ResponseEntity
          .status(HttpStatus.BAD_REQUEST)
          .body(response);
    }

    try {

      LiftRideEvent event = new LiftRideEvent(liftRide, resortID, seasonID, dayID, skierID);
      logger.info("Sending lift ride event to queue: {}", event);
      messageProducer.sendLiftRideEvent(event);


      ResponseMsg response = new ResponseMsg();
      response.setMessage("Write successful");
      return ResponseEntity
          .status(HttpStatus.CREATED)
          .body(response);
    } catch (Exception e) {

      logger.error("Failed to send message to queue", e);
      ResponseMsg response = new ResponseMsg();
      response.setMessage("Server error");
      return ResponseEntity
          .status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(response);
    }
  }

  private boolean isValidUrlParameters(int resortID, String seasonID, String dayID, int skierID) {

    if (resortID < 1 || resortID > 10) {
      return false;
    }


    if (!seasonID.equals("2025")) {
      return false;
    }


    if (!dayID.equals("1")) {
      return false;
    }


    if (skierID < 1 || skierID > 100000) {
      return false;
    }

    return true;
  }

  private boolean isValidLiftRide(LiftRide liftRide) {

    return !(liftRide.getLiftID() < 1 || liftRide.getLiftID() > 40 ||
        liftRide.getTime() < 1 || liftRide.getTime() > 360);
  }
}
