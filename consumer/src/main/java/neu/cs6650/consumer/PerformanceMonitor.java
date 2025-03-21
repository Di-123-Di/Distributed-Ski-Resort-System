package neu.cs6650.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class PerformanceMonitor {
  private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitor.class);

  private final SkierDataStore dataStore;
  private final ScheduledExecutorService scheduler;
  private final AtomicInteger lastProcessedCount = new AtomicInteger(0);

  public PerformanceMonitor(SkierDataStore dataStore) {
    this.dataStore = dataStore;
    this.scheduler = Executors.newScheduledThreadPool(1);
  }


  public void startMonitoring(int intervalSeconds) {

    lastProcessedCount.set(dataStore.getTotalProcessedMessages());


    scheduler.scheduleAtFixedRate(() -> {
      int currentCount = dataStore.getTotalProcessedMessages();
      int previousCount = lastProcessedCount.getAndSet(currentCount);
      int messagesInInterval = currentCount - previousCount;


      double ratePerSecond = (double) messagesInInterval / intervalSeconds;

      logger.info("====== Performance Statistics ======");
      logger.info("Total processed messages: {}", currentCount);
      logger.info("Unique skiers: {}", dataStore.getUniqueSkiersCount());
      logger.info("Processing rate: {}/sec", String.format("%.2f", ratePerSecond));
      logger.info("=====================================");

    }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
  }


  public void stopMonitoring() {
    scheduler.shutdown();
    try {
      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
