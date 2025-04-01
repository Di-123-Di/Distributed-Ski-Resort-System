package ski.servlet.launcher;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
public class TomcatLauncher {
  private static final Logger logger = LoggerFactory.getLogger(TomcatLauncher.class);
  public static void main(String[] args) throws Exception {
    String rabbitMQHost = "localhost";
    String rabbitMQUsername = "myuser";
    String rabbitMQPassword = "mypassword";
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (arg.startsWith("--rabbitmq.host=")) {
        rabbitMQHost = arg.substring("--rabbitmq.host=".length());
      }
      else if (arg.startsWith("--rabbitmq.username=")) {
        rabbitMQUsername = arg.substring("--rabbitmq.username=".length());
      }
      else if (arg.startsWith("--rabbitmq.password=")) {
        rabbitMQPassword = arg.substring("--rabbitmq.password=".length());
      }
      else if ("--rabbitmq.host".equals(arg) && i + 1 < args.length) {
        rabbitMQHost = args[++i];
      }
      else if ("--rabbitmq.username".equals(arg) && i + 1 < args.length) {
        rabbitMQUsername = args[++i];
      }
      else if ("--rabbitmq.password".equals(arg) && i + 1 < args.length) {
        rabbitMQPassword = args[++i];
      }
    }
    logger.info("Parsed RabbitMQ config: host={}, username={}", rabbitMQHost, rabbitMQUsername);
    System.setProperty("RABBITMQ_HOST", rabbitMQHost);
    System.setProperty("RABBITMQ_USERNAME", rabbitMQUsername);
    System.setProperty("RABBITMQ_PASSWORD", rabbitMQPassword);
    logger.info("Starting embedded Tomcat with RabbitMQ host: {}", rabbitMQHost);
    Tomcat tomcat = new Tomcat();
    tomcat.setPort(8081);


    tomcat.getConnector().setProperty("address", "0.0.0.0");

    Path tempPath = Files.createTempDirectory("tomcat-base-dir");
    tomcat.setBaseDir(tempPath.toString());
    String webappDirLocation = "src/main/webapp/";
    File webappDir = new File(webappDirLocation);
    if (!webappDir.exists()) {
      webappDirLocation = "webapp";
      webappDir = new File(webappDirLocation);
      if (!webappDir.exists()) {
        webappDir.mkdir();
      }
    }
    Context context = tomcat.addWebapp("", webappDir.getAbsolutePath());
    StandardContext standardContext = (StandardContext) context;
    standardContext.setTldValidation(false);
    standardContext.setXmlValidation(false);
    File classesDir = new File("target/classes");
    if (classesDir.exists()) {
      WebResourceRoot resources = new StandardRoot(context);
      resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
          classesDir.getAbsolutePath(), "/"));
      context.setResources(resources);
    }
    tomcat.start();
    logger.info("Embedded Tomcat started successfully on port 8081");
    tomcat.getServer().await();
  }
}
