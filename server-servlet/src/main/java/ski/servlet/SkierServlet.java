package ski.servlet;

import com.google.gson.Gson;
import io.swagger.client.model.LiftRide;
import ski.servlet.config.ApplicationConfig;
import ski.servlet.service.MessageProducer;
import ski.servlet.service.SkierService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/skiers/*")
public class SkierServlet extends HttpServlet {
  private static final Logger logger = LoggerFactory.getLogger(SkierServlet.class);
  private final Gson gson = new Gson();
  private SkierService skierService;
  private MessageProducer messageProducer;

  @Override
  public void init() {
    String host = System.getenv("RABBITMQ_HOST");
    String username = System.getenv("RABBITMQ_USERNAME");
    String password = System.getenv("RABBITMQ_PASSWORD");
    int port = 5672;

    if (host == null || host.isEmpty()) {
      host = "34.216.220.114";
      logger.warn("RABBITMQ_HOST not set, using default: {}", host);
    }

    if (username == null || username.isEmpty()) {
      username = "myuser";
      logger.warn("RABBITMQ_USERNAME not set, using default: {}", username);
    }

    if (password == null || password.isEmpty()) {
      password = "mypassword";
      logger.warn("RABBITMQ_PASSWORD not set, using default");
    }

    try {
      String portStr = System.getenv("RABBITMQ_PORT");
      if (portStr != null && !portStr.isEmpty()) {
        port = Integer.parseInt(portStr);
      }
    } catch (NumberFormatException e) {
      logger.warn("Invalid RABBITMQ_PORT format, using default: 5672");
    }

    logger.info("SkierServlet initializing with RabbitMQ config: host={}, port={}, username={}",
        host, port, username);

    try {

      this.messageProducer = MessageProducer.getInstance(host, port, username, password);
      this.skierService = new SkierService(messageProducer);
      logger.info("SkierServlet initialized successfully with RabbitMQ connection");
    } catch (Exception e) {
      logger.error("Failed to initialize SkierServlet with RabbitMQ connection", e);
    }
  }

  private boolean validateUrlPath(String urlPath) {
    if (urlPath == null || urlPath.isEmpty()) {
      return false;
    }

    String[] urlParts = urlPath.split("/");
    if (urlParts.length != 8) {
      return false;
    }

    try {
      int resortID = Integer.parseInt(urlParts[1]);
      if (resortID < 1 || resortID > 10) return false;

      if (!urlParts[2].equals("seasons")) return false;

      if (!urlParts[3].equals("2025")) return false;

      if (!urlParts[4].equals("days")) return false;

      int dayID = Integer.parseInt(urlParts[5]);
      if (dayID != 1) return false;

      if (!urlParts[6].equals("skiers")) return false;

      int skierID = Integer.parseInt(urlParts[7]);
      if (skierID < 1 || skierID > 100000) return false;

      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private boolean isValidLiftRide(LiftRide liftRide) {
    return !(liftRide.getLiftID() < 1 || liftRide.getLiftID() > 40 ||
        liftRide.getTime() < 1 || liftRide.getTime() > 360);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
    res.setContentType("application/json");
    PrintWriter out = res.getWriter();

    String urlPath = req.getPathInfo();

    logger.debug("Received POST request with URL path: {}", urlPath);

    if (!validateUrlPath(urlPath)) {
      logger.warn("Invalid URL path: {}", urlPath);
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      out.write(gson.toJson(new Message("Invalid URL path")));
      return;
    }

    try {
      String[] urlParts = urlPath.split("/");
      int resortID = Integer.parseInt(urlParts[1]);
      String seasonID = urlParts[3];
      String dayID = urlParts[5];
      int skierID = Integer.parseInt(urlParts[7]);

      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = req.getReader().readLine()) != null) {
        sb.append(line);
      }

      LiftRide liftRide = gson.fromJson(sb.toString(), LiftRide.class);

      if (!isValidLiftRide(liftRide)) {
        logger.warn("Invalid lift ride: liftID={}, time={}",
            liftRide.getLiftID(), liftRide.getTime());
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        out.write(gson.toJson(new Message("Invalid request body")));
        return;
      }

      logger.info("Processing lift ride: skierID={}, resortID={}, liftID={}",
          skierID, resortID, liftRide.getLiftID());

      skierService.processLiftRide(liftRide, resortID, seasonID, dayID, skierID);

      logger.info("Lift ride processed successfully");

      res.setStatus(HttpServletResponse.SC_CREATED);
      out.write(gson.toJson(new Message("Write successful")));

    } catch (Exception e) {
      logger.error("Error processing request", e);
      res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      out.write(gson.toJson(new Message("Server error: " + e.getMessage())));
    }
  }

  @Override
  public void destroy() {
    if (messageProducer != null) {
      messageProducer.cleanup();
    }
    logger.info("SkierServlet destroyed");
  }
}

class Message {
  private String message;

  public Message(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
