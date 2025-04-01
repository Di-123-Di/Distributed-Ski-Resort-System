package ski.servlet;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(urlPatterns = {"/", "/health", "/health/"})
public class HealthCheckServlet extends HttpServlet {
  private static final Logger logger = LoggerFactory.getLogger(HealthCheckServlet.class);

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    logger.info("Health check endpoint called");
    resp.setContentType("text/plain");
    resp.getWriter().write("OK");
  }
}
