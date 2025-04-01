package neu.cs6650.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerConfig {
  private static final Logger logger = LoggerFactory.getLogger(ConsumerConfig.class);

  private String host = "localhost";
  private int port = 5672;
  private String username = "myuser";
  private String password = "mypassword";
  private String queueName = "ski-rides";
  private int threadCount = 256;

  public ConsumerConfig(String[] args) {
    parseArgs(args);
  }

  private void parseArgs(String[] args) {
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--host":
          if (i + 1 < args.length) {
            host = args[++i];
          }
          break;
        case "--port":
          if (i + 1 < args.length) {
            port = Integer.parseInt(args[++i]);
          }
          break;
        case "--username":
          if (i + 1 < args.length) {
            username = args[++i];
          }
          break;
        case "--password":
          if (i + 1 < args.length) {
            password = args[++i];
          }
          break;
        case "--queue":
          if (i + 1 < args.length) {
            queueName = args[++i];
          }
          break;
        case "--threads":
          if (i + 1 < args.length) {
            threadCount = Integer.parseInt(args[++i]);
          }
          break;
        default:
          logger.warn("Unknown argument: {}", args[i]);
      }
    }

    logger.info("Configuration: host={}, port={}, username={}, queue={}, threads={}",
        host, port, username, queueName, threadCount);
  }


  public String getHost() { return host; }
  public int getPort() { return port; }
  public String getUsername() { return username; }
  public String getPassword() { return password; }
  public String getQueueName() { return queueName; }
  public int getThreadCount() { return threadCount; }
}