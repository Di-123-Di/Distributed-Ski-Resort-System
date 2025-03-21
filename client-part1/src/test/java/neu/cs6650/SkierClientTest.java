package neu.cs6650;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;

public class SkierClientTest {
  public static void main(String[] args) {
    String basePath = "http://44.246.77.104:8080/assignment1-1.0-SNAPSHOT";

    try {

      ApiClient apiClient = new ApiClient();
      apiClient.setBasePath(basePath);


      SkiersApi skiersApi = new SkiersApi(apiClient);


      LiftRide liftRide = new LiftRide();
      liftRide.setTime(217);
      liftRide.setLiftID(21);


      skiersApi.writeNewLiftRide(liftRide, 1, "2025", "1", 1);
      System.out.println("Successfully sent lift ride!");

    } catch (ApiException e) {
      System.err.println("Error sending lift ride!");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Response body: " + e.getResponseBody());
      System.err.println("Error message: " + e.getMessage());
      e.printStackTrace();
    }
  }

}
