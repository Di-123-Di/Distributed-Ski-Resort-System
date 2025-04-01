package ski.servlet.config;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebListener
public class ApplicationConfig implements ServletContextListener {
  private static final Logger logger = LoggerFactory.getLogger(ApplicationConfig.class);

  private static String rabbitmqHost = "localhost";
  private static int rabbitmqPort = 5672;
  private static String rabbitmqUsername = "myuser";
  private static String rabbitmqPassword = "mypassword";

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    ServletContext context = sce.getServletContext();

    String envHost = System.getenv("RABBITMQ_HOST");
    if (envHost != null && !envHost.isEmpty()) {
      rabbitmqHost = envHost;
    }

    String envPort = System.getenv("RABBITMQ_PORT");
    if (envPort != null && !envPort.isEmpty()) {
      try {
        rabbitmqPort = Integer.parseInt(envPort);
      } catch (NumberFormatException e) {
        logger.warn("Invalid RABBITMQ_PORT: {}, using default: {}", envPort, rabbitmqPort);
      }
    }

    String envUsername = System.getenv("RABBITMQ_USERNAME");
    if (envUsername != null && !envUsername.isEmpty()) {
      rabbitmqUsername = envUsername;
    }

    String envPassword = System.getenv("RABBITMQ_PASSWORD");
    if (envPassword != null && !envPassword.isEmpty()) {
      rabbitmqPassword = envPassword;
    }

    logger.info("Application initialized with RabbitMQ config: host={}, port={}, username={}",
        rabbitmqHost, rabbitmqPort, rabbitmqUsername);
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {

    logger.info("Application shutting down");
  }

  public static String getRabbitmqHost() {
    return rabbitmqHost;
  }

  public static int getRabbitmqPort() {
    return rabbitmqPort;
  }

  public static String getRabbitmqUsername() {
    return rabbitmqUsername;
  }

  public static String getRabbitmqPassword() {
    return rabbitmqPassword;
  }
}