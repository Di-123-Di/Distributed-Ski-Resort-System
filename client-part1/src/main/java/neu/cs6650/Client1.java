package neu.cs6650;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.BlockingQueue;

public class Client1 {
  private static String BASE_PATH = "http://localhost:8081";
  private static final int TOTAL_REQUESTS = 200000;
  private static final int INITIAL_THREADS = 48;
  private static final int REQUESTS_PER_THREAD = 1000;

  public static void main(String[] args) {
    for (int i = 0; i < args.length; i++) {
      if ("--server".equals(args[i]) && i + 1 < args.length) {
        BASE_PATH = args[++i];
      }
    }

    long startTime = System.currentTimeMillis();
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);


    Thread progressThread = new Thread(() -> {
      while (!Thread.currentThread().isInterrupted()) {
        try {
          Thread.sleep(1000);
          long currentTime = System.currentTimeMillis();
          long elapsedSeconds = (currentTime - startTime) / 1000;
          int total = successCount.get() + failureCount.get();
          double completionPercentage = (total * 100.0) / TOTAL_REQUESTS;
          System.out.printf("Progress: %.2f%% - Success: %d, Failed: %d, Elapsed: %ds, Current Rate: %.2f req/s%n",
              completionPercentage,
              successCount.get(),
              failureCount.get(),
              elapsedSeconds,
              elapsedSeconds > 0 ? total / (double)elapsedSeconds : 0);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    });
    progressThread.setDaemon(true);
    progressThread.start();

    LiftRideEventGenerator eventGenerator = new LiftRideEventGenerator(TOTAL_REQUESTS);
    BlockingQueue<LiftRideEventGenerator.LiftRideEvent> eventQueue = eventGenerator.getEventQueue();
    Thread generatorThread = new Thread(eventGenerator);
    generatorThread.start();

    List<Thread> initialThreads = processRequests(
        INITIAL_THREADS,
        REQUESTS_PER_THREAD,
        BASE_PATH,
        eventQueue,
        successCount,
        failureCount
    );

    waitForThreads(initialThreads);

    int remainingRequests = TOTAL_REQUESTS - (INITIAL_THREADS * REQUESTS_PER_THREAD);
    int additionalThreads = remainingRequests / REQUESTS_PER_THREAD;

    List<Thread> remainingThreads = processRequests(
        additionalThreads,
        REQUESTS_PER_THREAD,
        BASE_PATH,
        eventQueue,
        successCount,
        failureCount
    );

    waitForThreads(remainingThreads);
    progressThread.interrupt();

    long endTime = System.currentTimeMillis();
    long wallTime = endTime - startTime;

    printPerformanceStats(successCount, failureCount, wallTime);
  }

  private static List<Thread> processRequests(
      int threadCount,
      int requestsPerThread,
      String basePath,
      BlockingQueue<LiftRideEventGenerator.LiftRideEvent> eventQueue,
      AtomicInteger successCount,
      AtomicInteger failureCount
  ) {
    List<Thread> threads = new ArrayList<>();
    for (int i = 0; i < threadCount; i++) {
      Thread thread = new Thread(new PostRequestThread1(
          requestsPerThread,
          basePath,
          eventQueue,
          successCount,
          failureCount
      ));
      threads.add(thread);
      thread.start();
    }
    return threads;
  }

  private static void waitForThreads(List<Thread> threads) {
    for (Thread thread : threads) {
      try {
        thread.join(60000);
        if (thread.isAlive()) {
          System.out.println("Thread is taking too long, continuing...");
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private static void printPerformanceStats(
      AtomicInteger successCount,
      AtomicInteger failureCount,
      long wallTime
  ) {
    System.out.println("=== Client Configuration ===");
    System.out.println("Initial Threads: " + INITIAL_THREADS);
    int remainingRequests = TOTAL_REQUESTS - (INITIAL_THREADS * REQUESTS_PER_THREAD);
    int additionalThreads = remainingRequests / REQUESTS_PER_THREAD;
    System.out.println("Additional Threads: " + additionalThreads);
    System.out.println("Total Threads Used: " + (INITIAL_THREADS + additionalThreads));
    System.out.println("Requests per Thread: " + REQUESTS_PER_THREAD);

    System.out.println("\n=== Performance Results ===");
    System.out.println("Successful requests: " + successCount.get());
    System.out.println("Failed requests: " + failureCount.get());
    System.out.println("Wall Time: " + wallTime + " milliseconds");
    System.out.println("Throughput: " + (successCount.get() / (wallTime / 1000.0)) + " requests/second");
  }
}