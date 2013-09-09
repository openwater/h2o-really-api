import com.fasterxml.jackson.databind.ObjectMapper;
import com.jolbox.bonecp.BoneCP;
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

    public final BoneCP connectionPool;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setHeader("Content-Type", "application/json");

        boolean compact = Boolean.parseBoolean(req.getParameter("compact"));
        int pageSize = 0;
        int page = 0;

        try {
            pageSize = Integer.parseInt(req.getParameter("page_size"));
        } catch (NumberFormatException nfe) {
            //
        }
        try {
            page = Integer.parseInt(req.getParameter("page"));
        } catch (NumberFormatException nfe) {
            //
        }

        // This may mean we create an ArrayList that's larger than we need, but I'm OK with that
        MeasurementsAPI.JsonResponse jsonResponse = new MeasurementsAPI.JsonResponse();
        Connection connection = null;
        try {
            connection = this.connectionPool.getConnection();
            jsonResponse.results = MeasurementsAPI.getFeatures(
                connection, compact, page, pageSize
            );
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        try {
            ObjectMapper mapper = MeasurementsAPI.getMapper();
            mapper.writeValue(resp.getOutputStream(), jsonResponse);
        } catch (IOException ioe) {
            // do nothing
        }
    }

    public JettyServer() throws ClassNotFoundException, SQLException, URISyntaxException {
        this.connectionPool = Database.getConnectionPool();
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
