import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Basic server using embedded Jetty.
 */
public class JettyServer extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        MeasurementsAPI.JsonResponse jsonResponse = null;
        try {
            // noinspection unchecked
            Map<String, String> params = new HashMap<String, String>();
            for (Object obj : req.getParameterMap().keySet()) {
                String key = obj.toString();
                params.put(key, req.getParameter(key));
            }
            jsonResponse = MeasurementsAPI.getResponse(params);
            if (jsonResponse.previous != null) {
                jsonResponse.previous = req.getRequestURL() + jsonResponse.previous;
            }
            if (jsonResponse.next != null) {
                jsonResponse.next = req.getRequestURL() + jsonResponse.next;
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        resp.setHeader("Content-Type", "application/json");

        try {
            ObjectMapper mapper = MeasurementsAPI.getMapper();
            mapper.writeValue(resp.getOutputStream(), jsonResponse);
        } catch (IOException ioe) {
            // do nothing
        }
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(Integer.valueOf(System.getenv("PORT")));
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new JettyServer()), "/*");
        server.start();
        server.join();
    }
}
