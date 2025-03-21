package neu.cs6650;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.SkiersApi;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import neu.cs6650.LiftRideEventGenerator.LiftRideEvent;

public class PostRequestThread1 implements Runnable {

  private final int numRequests;
  private final String basePath;
  private final BlockingQueue<LiftRideEvent> eventQueue;
  private final AtomicInteger successCount;
  private final AtomicInteger failureCount;
  private static final int MAX_RETRIES = 5;

  public PostRequestThread1(int numRequests,
      String basePath,
      BlockingQueue<LiftRideEvent> eventQueue,
      AtomicInteger successCount,
      AtomicInteger failureCount) {
    this.numRequests = numRequests;
    this.basePath = basePath;
    this.eventQueue = eventQueue;
    this.successCount = successCount;
    this.failureCount = failureCount;
  }

  @Override
  public void run() {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(basePath);

    apiClient.getHttpClient().setConnectTimeout(5000, TimeUnit.MILLISECONDS);
    apiClient.getHttpClient().setReadTimeout(5000, TimeUnit.MILLISECONDS);

    SkiersApi skiersApi = new SkiersApi(apiClient);

    for (int i = 0; i < numRequests; i++) {
      LiftRideEvent event;
      try {
        event = eventQueue.take();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }

      boolean success = false;
      int retries = 0;

      while (!success && retries < MAX_RETRIES) {
        try {
          skiersApi.writeNewLiftRide(event.liftRide,
              event.resortID,
              "2025",
              "1",
              event.skierID);
          success = true;
          successCount.incrementAndGet();
        } catch (ApiException e) {
          retries++;
          if (e.getCode() >= 400 && retries >= MAX_RETRIES) {
            failureCount.incrementAndGet();
            break;
          }
          try {
            Thread.sleep(100);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            break;
          }
        } catch (Exception e) {
          // Handle unexpected exceptions
          retries++;
          if (retries >= MAX_RETRIES) {
            failureCount.incrementAndGet();
            break;
          }
          try {
            Thread.sleep(100);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            break;
          }
        }
      }
    }
  }
}