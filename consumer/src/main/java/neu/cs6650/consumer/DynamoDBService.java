package neu.cs6650.consumer;

import io.swagger.client.model.LiftRide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class DynamoDBService {
  private static final Logger logger = LoggerFactory.getLogger(DynamoDBService.class);

  private final DynamoDbClient dynamoDbClient;
  private final DynamoDbEnhancedClient enhancedClient;
  private final String tableName = "SkierLiftRides";
  private final AtomicInteger totalProcessedMessages = new AtomicInteger(0);
  private final AtomicInteger uniqueSkiers = new AtomicInteger(0);


  private final Map<Integer, Boolean> skierCache = new ConcurrentHashMap<>();


  private static final int BATCH_SIZE = 25;
  private final List<Map<String, AttributeValue>> itemBatch = new ArrayList<>();
  private final ReentrantLock batchLock = new ReentrantLock();

  public DynamoDBService() {
    this.dynamoDbClient = DynamoDbClient.builder()
        .region(Region.US_WEST_2)
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build();

    this.enhancedClient = DynamoDbEnhancedClient.builder()
        .dynamoDbClient(dynamoDbClient)
        .build();

    logger.info("DynamoDB service initialized");
  }

  /**
   * Add a single lift ride to the batch queue and flush if batch size is reached
   */
  public void addLiftRide(int skierID, LiftRide liftRide, int resortID, String seasonID, String dayID) {
    try {

      String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
      String timestampLiftID = timestamp + "#" + liftRide.getLiftID();
      String resortDay = resortID + "#" + dayID;


      int vertical = liftRide.getLiftID() * 10;

      Map<String, AttributeValue> item = new HashMap<>();
      item.put("skierID", AttributeValue.builder().n(String.valueOf(skierID)).build());
      item.put("timestamp_liftID", AttributeValue.builder().s(timestampLiftID).build());
      item.put("resortID", AttributeValue.builder().n(String.valueOf(resortID)).build());
      item.put("seasonID", AttributeValue.builder().s(seasonID).build());
      item.put("dayID", AttributeValue.builder().s(dayID).build());
      item.put("liftID", AttributeValue.builder().n(String.valueOf(liftRide.getLiftID())).build());
      item.put("time", AttributeValue.builder().n(String.valueOf(liftRide.getTime())).build());
      item.put("vertical", AttributeValue.builder().n(String.valueOf(vertical)).build());
      item.put("timestamp", AttributeValue.builder().s(timestamp).build());
      item.put("resortDay", AttributeValue.builder().s(resortDay).build());


      if (!skierCache.containsKey(skierID)) {
        skierCache.put(skierID, true);
        uniqueSkiers.incrementAndGet();
      }


      batchLock.lock();
      try {
        itemBatch.add(item);
        totalProcessedMessages.incrementAndGet();

        if (itemBatch.size() >= BATCH_SIZE) {
          flushBatch();
        }
      } finally {
        batchLock.unlock();
      }
    } catch (Exception e) {
      logger.error("Error adding lift ride to DynamoDB", e);
    }
  }

  /**
   * Add multiple lift rides in a batch
   */
  public void batchAddLiftRides(List<LiftRideEvent> events) {
    for (LiftRideEvent event : events) {
      addLiftRide(
          event.getSkierID(),
          event.getLiftRide(),
          event.getResortID(),
          event.getSeasonID(),
          event.getDayID()
      );
    }
  }

  /**
   * Flush the current batch to DynamoDB
   */
  public void flushBatch() {
    batchLock.lock();
    try {
      if (itemBatch.isEmpty()) {
        return;
      }

      List<WriteRequest> writeRequests = new ArrayList<>();
      for (Map<String, AttributeValue> item : itemBatch) {
        PutRequest putRequest = PutRequest.builder().item(item).build();
        WriteRequest writeRequest = WriteRequest.builder().putRequest(putRequest).build();
        writeRequests.add(writeRequest);
      }

      Map<String, List<WriteRequest>> requestItems = new HashMap<>();
      requestItems.put(tableName, writeRequests);

      BatchWriteItemRequest batchWriteItemRequest = BatchWriteItemRequest.builder()
          .requestItems(requestItems)
          .build();


      dynamoDbClient.batchWriteItem(batchWriteItemRequest);

      logger.debug("Flushed batch of {} items to DynamoDB", itemBatch.size());
      itemBatch.clear();
    } catch (Exception e) {
      logger.error("Error flushing batch to DynamoDB", e);
    } finally {
      batchLock.unlock();
    }
  }

  /**
   * Get the number of days a skier has skied this season
   */
  public int getSkierDaysCount(int skierID, String seasonID) {
    try {

      flushBatch();

      Map<String, AttributeValue> expressionValues = new HashMap<>();
      expressionValues.put(":skierID", AttributeValue.builder().n(String.valueOf(skierID)).build());
      expressionValues.put(":seasonID", AttributeValue.builder().s(seasonID).build());


      Map<String, String> expressionNames = new HashMap<>();
      expressionNames.put("#seasonID", "seasonID");


      software.amazon.awssdk.services.dynamodb.model.QueryRequest queryRequest =
          software.amazon.awssdk.services.dynamodb.model.QueryRequest.builder()
              .tableName(tableName)
              .keyConditionExpression("skierID = :skierID")
              .filterExpression("#seasonID = :seasonID")
              .expressionAttributeNames(expressionNames)
              .expressionAttributeValues(expressionValues)
              .build();

      List<Map<String, AttributeValue>> items = dynamoDbClient.query(queryRequest).items();


      Map<String, Boolean> uniqueDays = new HashMap<>();
      for (Map<String, AttributeValue> item : items) {
        AttributeValue dayValue = item.get("dayID");
        if (dayValue != null) {
          uniqueDays.put(dayValue.s(), true);
        }
      }

      return uniqueDays.size();
    } catch (Exception e) {
      logger.error("Error getting skier days count", e);
      return 0;
    }
  }

  /**
   * Get the vertical totals for each ski day for a skier
   */
  public Map<String, Integer> getSkierVerticalTotals(int skierID, String seasonID) {
    try {

      flushBatch();

      Map<String, AttributeValue> expressionValues = new HashMap<>();
      expressionValues.put(":skierID", AttributeValue.builder().n(String.valueOf(skierID)).build());
      expressionValues.put(":seasonID", AttributeValue.builder().s(seasonID).build());

      Map<String, String> expressionNames = new HashMap<>();
      expressionNames.put("#seasonID", "seasonID");

      software.amazon.awssdk.services.dynamodb.model.QueryRequest queryRequest =
          software.amazon.awssdk.services.dynamodb.model.QueryRequest.builder()
              .tableName(tableName)
              .keyConditionExpression("skierID = :skierID")
              .filterExpression("#seasonID = :seasonID")
              .expressionAttributeNames(expressionNames)
              .expressionAttributeValues(expressionValues)
              .build();

      List<Map<String, AttributeValue>> items = dynamoDbClient.query(queryRequest).items();


      Map<String, Integer> verticalByDay = new HashMap<>();
      for (Map<String, AttributeValue> item : items) {
        AttributeValue dayValue = item.get("dayID");
        AttributeValue verticalValue = item.get("vertical");

        if (dayValue != null && verticalValue != null) {
          String day = dayValue.s();
          int vertical = Integer.parseInt(verticalValue.n());

          verticalByDay.put(day, verticalByDay.getOrDefault(day, 0) + vertical);
        }
      }

      return verticalByDay;
    } catch (Exception e) {
      logger.error("Error getting skier vertical totals", e);
      return new HashMap<>();
    }
  }

  /**
   * Get the lifts a skier rode on a specific day
   */
  public List<Integer> getSkierLifts(int skierID, String seasonID, String dayID) {
    try {

      flushBatch();

      Map<String, AttributeValue> expressionValues = new HashMap<>();
      expressionValues.put(":skierID", AttributeValue.builder().n(String.valueOf(skierID)).build());
      expressionValues.put(":seasonID", AttributeValue.builder().s(seasonID).build());
      expressionValues.put(":dayID", AttributeValue.builder().s(dayID).build());

      Map<String, String> expressionNames = new HashMap<>();
      expressionNames.put("#seasonID", "seasonID");
      expressionNames.put("#dayID", "dayID");

      software.amazon.awssdk.services.dynamodb.model.QueryRequest queryRequest =
          software.amazon.awssdk.services.dynamodb.model.QueryRequest.builder()
              .tableName(tableName)
              .keyConditionExpression("skierID = :skierID")
              .filterExpression("#seasonID = :seasonID AND #dayID = :dayID")
              .expressionAttributeNames(expressionNames)
              .expressionAttributeValues(expressionValues)
              .build();

      List<Map<String, AttributeValue>> items = dynamoDbClient.query(queryRequest).items();


      List<Integer> lifts = new ArrayList<>();
      for (Map<String, AttributeValue> item : items) {
        AttributeValue liftValue = item.get("liftID");
        if (liftValue != null) {
          lifts.add(Integer.parseInt(liftValue.n()));
        }
      }

      return lifts;
    } catch (Exception e) {
      logger.error("Error getting skier lifts", e);
      return new ArrayList<>();
    }
  }

  /**
   * Get the number of unique skiers at a resort on a specific day
   */
  public int getUniqueSkiersCount(int resortID, String dayID) {
    try {

      flushBatch();

      String resortDay = resortID + "#" + dayID;
      Map<String, AttributeValue> expressionValues = new HashMap<>();
      expressionValues.put(":resortDay", AttributeValue.builder().s(resortDay).build());

      software.amazon.awssdk.services.dynamodb.model.QueryRequest queryRequest =
          software.amazon.awssdk.services.dynamodb.model.QueryRequest.builder()
              .tableName(tableName)
              .indexName("ResortDayIndex")
              .keyConditionExpression("resortDay = :resortDay")
              .expressionAttributeValues(expressionValues)
              .build();

      List<Map<String, AttributeValue>> items = dynamoDbClient.query(queryRequest).items();


      Map<String, Boolean> uniqueSkiers = new HashMap<>();
      for (Map<String, AttributeValue> item : items) {
        AttributeValue skierValue = item.get("skierID");
        if (skierValue != null) {
          uniqueSkiers.put(skierValue.n(), true);
        }
      }

      return uniqueSkiers.size();
    } catch (Exception e) {
      logger.error("Error getting unique skiers count for resort and day", e);
      return 0;
    }
  }

  public int getTotalProcessedMessages() {
    return totalProcessedMessages.get();
  }

  public int getUniqueSkiersCount() {
    return uniqueSkiers.get();
  }

  public void shutdown() {

    flushBatch();

    if (dynamoDbClient != null) {
      dynamoDbClient.close();
    }
  }
}