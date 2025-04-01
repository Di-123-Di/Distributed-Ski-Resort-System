package ski.servlet.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class ResourceConfig implements ServletContextListener {
  private static final Logger logger = LoggerFactory.getLogger(ResourceConfig.class);

  @Override
  public void contextInitialized(ServletContextEvent sce) {

    String rabbitMQHost = System.getProperty("RABBITMQ_HOST", "localhost");
    String rabbitMQUsername = System.getProperty("RABBITMQ_USERNAME", "myuser");
    String rabbitMQPassword = System.getProperty("RABBITMQ_PASSWORD", "mypassword");


    sce.getServletContext().setAttribute("rabbitmq.host", rabbitMQHost);
    sce.getServletContext().setAttribute("rabbitmq.username", rabbitMQUsername);
    sce.getServletContext().setAttribute("rabbitmq.password", rabbitMQPassword);

    logger.info("Application initialized with RabbitMQ config: host={}, username={}",
        rabbitMQHost, rabbitMQUsername);
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    logger.info("Application context destroyed");
  }
}
