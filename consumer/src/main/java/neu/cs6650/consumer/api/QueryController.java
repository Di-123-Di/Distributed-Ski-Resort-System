package neu.cs6650.consumer.api;

import neu.cs6650.consumer.DynamoDBService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class QueryController {
  private static final Logger logger = LoggerFactory.getLogger(QueryController.class);

  private final DynamoDBService dynamoDBService;

  public QueryController(DynamoDBService dynamoDBService) {
    this.dynamoDBService = dynamoDBService;
  }

  /**
   * Get the number of days a skier has skied in a season
   */
  @GetMapping("/skiers/{skierId}/seasons/{seasonId}/days/count")
  public ResponseEntity<Integer> getSkierDaysCount(
      @PathVariable("skierId") int skierId,
      @PathVariable("seasonId") String seasonId) {
    logger.info("Getting days count for skier {} in season {}", skierId, seasonId);
    int daysCount = dynamoDBService.getSkierDaysCount(skierId, seasonId);
    return ResponseEntity.ok(daysCount);
  }

  /**
   * Get the vertical totals for each ski day for a skier
   */
  @GetMapping("/skiers/{skierId}/seasons/{seasonId}/vertical")
  public ResponseEntity<Map<String, Integer>> getSkierVerticalTotals(
      @PathVariable("skierId") int skierId,
      @PathVariable("seasonId") String seasonId) {
    logger.info("Getting vertical totals for skier {} in season {}", skierId, seasonId);
    Map<String, Integer> verticalTotals = dynamoDBService.getSkierVerticalTotals(skierId, seasonId);
    return ResponseEntity.ok(verticalTotals);
  }

  /**
   * Get the lifts a skier rode on a specific day
   */
  @GetMapping("/skiers/{skierId}/seasons/{seasonId}/days/{dayId}/lifts")
  public ResponseEntity<List<Integer>> getSkierLifts(
      @PathVariable("skierId") int skierId,
      @PathVariable("seasonId") String seasonId,
      @PathVariable("dayId") String dayId) {
    logger.info("Getting lifts for skier {} on day {} in season {}", skierId, dayId, seasonId);
    List<Integer> lifts = dynamoDBService.getSkierLifts(skierId, seasonId, dayId);
    return ResponseEntity.ok(lifts);
  }

  /**
   * Get the number of unique skiers at a resort on a specific day
   */
  @GetMapping("/resorts/{resortId}/seasons/{seasonId}/days/{dayId}/skiers/count")
  public ResponseEntity<Integer> getUniqueSkiersCount(
      @PathVariable("resortId") int resortId,
      @PathVariable("dayId") String dayId) {
    logger.info("Getting unique skiers count for resort {} on day {}", resortId, dayId);
    int skiersCount = dynamoDBService.getUniqueSkiersCount(resortId, dayId);
    return ResponseEntity.ok(skiersCount);
  }

  /**
   * Get total stats about processed messages
   */
  @GetMapping("/stats")
  public ResponseEntity<Stats> getStats() {
    Stats stats = new Stats(
        dynamoDBService.getTotalProcessedMessages(),
        dynamoDBService.getUniqueSkiersCount()
    );
    return ResponseEntity.ok(stats);
  }

  /**
   * Stats response class
   */
  public static class Stats {
    private final int totalProcessedMessages;
    private final int uniqueSkiers;

    public Stats(int totalProcessedMessages, int uniqueSkiers) {
      this.totalProcessedMessages = totalProcessedMessages;
      this.uniqueSkiers = uniqueSkiers;
    }

    public int getTotalProcessedMessages() {
      return totalProcessedMessages;
    }

    public int getUniqueSkiers() {
      return uniqueSkiers;
    }
  }
}
