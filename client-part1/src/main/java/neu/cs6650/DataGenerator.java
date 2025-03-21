package neu.cs6650;

import io.swagger.client.model.LiftRide;
import java.util.Random;

public class DataGenerator {
  private final Random random = new Random();


  public int getRandomSkierID() {
    return random.nextInt(100000) + 1;
  }

  public int getRandomResortID() {
    return random.nextInt(10) + 1;
  }

  public int getRandomLiftID() {
    return random.nextInt(40) + 1;
  }

  public int getRandomTime() {
    return random.nextInt(360) + 1;
  }


  public LiftRide generateLiftRide() {
    LiftRide liftRide = new LiftRide();
    liftRide.setTime(getRandomTime());
    liftRide.setLiftID(getRandomLiftID());
    return liftRide;
  }
}
