import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Basic server using embedded Jetty.
 */
public class JettyServer extends HttpServlet {

    private final Connection connection;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setHeader("Content-Type", "application/json");

        boolean compact = false;
        int pageSize, page;

        try {
            pageSize = Integer.parseInt(req.getParameter("page_size"));
        } catch (NumberFormatException nfe) {
            pageSize = 10;
        }
        try {
            page = Integer.parseInt(req.getParameter("page"));
        } catch (NumberFormatException nfe) {
            page = 1;
        }
        try {
            compact = Boolean.parseBoolean(req.getParameter("compact"));
        } catch (ArrayIndexOutOfBoundsException e) {
            //
        }

        // This may mean we create an ArrayList that's larger than we need, but I'm OK with that
        MeasurementsAPI.JsonResponse jsonResponse = new MeasurementsAPI.JsonResponse();
        jsonResponse.results = MeasurementsAPI.getFeatures(this.connection, compact, page, pageSize);

        try {
            ObjectMapper mapper = MeasurementsAPI.getMapper();
            mapper.writeValue(resp.getOutputStream(), jsonResponse);
        } catch (IOException ioe) {
            // do nothing
        }
    }

    public JettyServer() throws ClassNotFoundException, SQLException, URISyntaxException {
        this.connection = new Database().connection;
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(Integer.valueOf(System.getenv("PORT")));
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new JettyServer()),"/*");
        server.start();
        server.join();
    }
}
