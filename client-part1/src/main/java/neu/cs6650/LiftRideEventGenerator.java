package neu.cs6650;

import io.swagger.client.model.LiftRide;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class LiftRideEventGenerator implements Runnable {
  private final BlockingQueue<LiftRideEvent> eventQueue;
  private final int totalEvents;
  private final DataGenerator dataGenerator;
  private static final int QUEUE_SIZE = 100000;


  public static class LiftRideEvent {
    public final LiftRide liftRide;
    public final int skierID;
    public final int resortID;

    public LiftRideEvent(LiftRide liftRide, int skierID, int resortID) {
      this.liftRide = liftRide;
      this.skierID = skierID;
      this.resortID = resortID;
    }
  }

  public LiftRideEventGenerator(int totalEvents) {
    this.eventQueue = new LinkedBlockingQueue<>(QUEUE_SIZE);
    this.totalEvents = totalEvents;
    this.dataGenerator = new DataGenerator();
  }

  public BlockingQueue<LiftRideEvent> getEventQueue() {
    return eventQueue;
  }

  @Override
  public void run() {
    try {
      for (int i = 0; i < totalEvents; i++) {
        LiftRide liftRide = dataGenerator.generateLiftRide();
        int skierID = dataGenerator.getRandomSkierID();
        int resortID = dataGenerator.getRandomResortID();

        LiftRideEvent event = new LiftRideEvent(liftRide, skierID, resortID);
        eventQueue.put(event);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
