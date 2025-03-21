package neu.cs6650;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.BlockingQueue;

public class Client1 {
  private static final String BASE_PATH = "http://ski-resort-alb-1513429395.us-west-2.elb.amazonaws.com:8081";
  private static final int TOTAL_REQUESTS = 200000;
  private static final int INITIAL_THREADS = 32;
  private static final int REQUESTS_PER_THREAD = 1000;

  public static void main(String[] args) {

    long startTime = System.currentTimeMillis();
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);


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
        thread.join();
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
    System.out.println("Throughput: " + (TOTAL_REQUESTS / (wallTime / 1000.0)) + " requests/second");
  }
}
