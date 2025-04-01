package neu.cs6650.consumer;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
public class ConsumerApiApplication {


  public static void main(String[] args) {
    SpringApplication.run(ConsumerApiApplication.class, args);
  }


  @Bean
  public DynamoDBService dynamoDBService() {
    return new DynamoDBService();
  }
}

