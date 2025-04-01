package neu.cs6650.consumer;

import io.swagger.client.model.LiftRide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class SkierDataStore {
  private static final Logger logger = LoggerFactory.getLogger(SkierDataStore.class);


  private final Map<Integer, List<LiftRide>> skierRecords = new ConcurrentHashMap<>();
  private final AtomicInteger totalProcessedMessages = new AtomicInteger(0);
  private final AtomicInteger uniqueSkiers = new AtomicInteger(0);

  public void addLiftRide(int skierID, LiftRide liftRide) {

    skierRecords.computeIfAbsent(skierID, k -> {
      uniqueSkiers.incrementAndGet();
      return new CopyOnWriteArrayList<>();
    }).add(liftRide);

    int total = totalProcessedMessages.incrementAndGet();
  }

  public List<LiftRide> getSkierRecords(int skierID) {
    return skierRecords.getOrDefault(skierID, new CopyOnWriteArrayList<>());
  }

  public int getUniqueSkiersCount() {
    return uniqueSkiers.get();
  }

  public int getTotalProcessedMessages() {
    return totalProcessedMessages.get();
  }

  public Map<Integer, List<LiftRide>> getAllSkierRecords() {
    return skierRecords;
  }
}
