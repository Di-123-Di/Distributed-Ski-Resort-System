package neu.cs6650;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.SkiersApi;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import neu.cs6650.LiftRideEventGenerator.LiftRideEvent;

public class PostRequestThread1 implements Runnable {

  private final int numRequests;
  private final String basePath;
  private final BlockingQueue<LiftRideEvent> eventQueue;
  private final AtomicInteger successCount;
  private final AtomicInteger failureCount;
  private static final int MAX_RETRIES = 3;

  private static final int MAX_CONSECUTIVE_FAILURES = 20;
  private static final long BACKOFF_INITIAL_MS = 100;
  private static final long BACKOFF_MAX_MS = 2000;
  private static final AtomicInteger circuitFailureCount = new AtomicInteger(0);
  private static final AtomicLong lastCircuitResetTime = new AtomicLong(System.currentTimeMillis());
  private static final long CIRCUIT_RESET_INTERVAL_MS = 2000;

  private static final long BASE_INTERVAL_MS = 0;
  private static final int MAX_TOKENS = 10000;
  private static final AtomicInteger availableTokens = new AtomicInteger(MAX_TOKENS);
  private static final long TOKEN_REFILL_INTERVAL_MS = 50;
  private static final int TOKENS_PER_REFILL = 400;
  private static long lastTokenRefillTime = System.currentTimeMillis();

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

  private synchronized boolean getToken() {
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastTokenRefillTime > TOKEN_REFILL_INTERVAL_MS) {
      int intervals = (int)((currentTime - lastTokenRefillTime) / TOKEN_REFILL_INTERVAL_MS);
      int tokensToAdd = intervals * TOKENS_PER_REFILL;
      int newCount = Math.min(availableTokens.get() + tokensToAdd, MAX_TOKENS);
      availableTokens.set(newCount);
      lastTokenRefillTime = currentTime - (currentTime - lastTokenRefillTime) % TOKEN_REFILL_INTERVAL_MS;
    }

    if (availableTokens.get() > 0) {
      availableTokens.decrementAndGet();
      return true;
    }
    return false;
  }

  @Override
  public void run() {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(basePath);

    apiClient.getHttpClient().setConnectTimeout(1000, TimeUnit.MILLISECONDS);
    apiClient.getHttpClient().setReadTimeout(1000, TimeUnit.MILLISECONDS);

    SkiersApi skiersApi = new SkiersApi(apiClient);

    long backoffTime = BACKOFF_INITIAL_MS;
    boolean isCircuitOpen = false;

    for (int i = 0; i < numRequests; i++) {

      if (!getToken()) {
        try {
          Thread.sleep(5);
          continue;
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }

      if (isCircuitOpen) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCircuitResetTime.get() > CIRCUIT_RESET_INTERVAL_MS) {
          isCircuitOpen = false;
          circuitFailureCount.set(0);
          lastCircuitResetTime.set(currentTime);
          backoffTime = BACKOFF_INITIAL_MS;
          System.out.println("Circuit breaker reset, resuming requests.");
        } else {
          try {
            Thread.sleep(backoffTime / 2);
            i--;
            continue;
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }
        }
      }

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

          backoffTime = Math.max(BACKOFF_INITIAL_MS, backoffTime / 2);

          if (circuitFailureCount.get() > 0) {
            circuitFailureCount.set(0);
          }

        } catch (ApiException e) {
          retries++;

          int failures = circuitFailureCount.incrementAndGet();
          if (failures >= MAX_CONSECUTIVE_FAILURES) {
            isCircuitOpen = true;
            lastCircuitResetTime.set(System.currentTimeMillis());
            System.out.println("Circuit breaker opened due to server errors. Backing off.");
            backoffTime = BACKOFF_MAX_MS;
            break;
          }

          if (e.getCode() >= 400 && retries >= MAX_RETRIES) {
            failureCount.incrementAndGet();
            break;
          }

          try {
            Thread.sleep(backoffTime / 4);
            backoffTime = (long) Math.min(backoffTime * 1.5, BACKOFF_MAX_MS);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            break;
          }
        } catch (Exception e) {
          failureCount.incrementAndGet();
          break;
        }
      }
    }
  }
}