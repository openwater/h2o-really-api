import com.fasterxml.jackson.databind.ObjectMapper;
import org.simpleframework.http.Query;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.Server;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SimpleServer implements Container {

    private final java.sql.Connection connection;

    public static class Task implements Runnable {

        private final java.sql.Connection connection;
        private final Response response;
        private final Request request;

        public Task(java.sql.Connection connection, Request request, Response response) {
            this.connection = connection;
            this.response = response;
            this.request = request;
        }

        public void run() {
            try {
                PrintStream body = response.getPrintStream();
                Query query = request.getQuery();

                boolean compact = query.getBoolean("compact");
                int page = query.getInteger("page");
                int pageSize = query.getInteger("page_size");

                // This may mean we create an ArrayList that's larger than we need, but I'm OK with that
                MeasurementsAPI.JsonResponse jsonResponse = new MeasurementsAPI.JsonResponse();
                jsonResponse.results = MeasurementsAPI.getFeatures(this.connection, compact, page, pageSize);

                try {
                    ObjectMapper mapper = MeasurementsAPI.getMapper();
                    mapper.writeValue(body, jsonResponse);
                } catch (IOException ioe) {
                    // do nothing
                }

                long time = System.currentTimeMillis();
                response.setValue("Content-Type", "application/json");
                response.setValue("Server", "MeasurementsAPI/1.0 (Simple 5.1.5)");
                response.setDate("Date", time);
                response.setDate("Last-Modified", time);

                body.close();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private final Executor executor;

    public SimpleServer(int size) throws ClassNotFoundException, SQLException, URISyntaxException {
        this.executor = Executors.newFixedThreadPool(size);
        this.connection = new Database().connection;
    }

    public void handle(Request request, Response response) {
        Task task = new Task(connection, request, response);

        executor.execute(task);
    }

    public static void main(String[] list) throws Exception {
        Container container = new SimpleServer(10);
        Server server = new ContainerServer(container);
        Connection connection = new SocketConnection(server);
        SocketAddress address = new InetSocketAddress(Integer.valueOf(System.getenv("PORT")));

        connection.connect(address);
    }
}