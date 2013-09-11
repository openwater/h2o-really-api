import com.fasterxml.jackson.databind.ObjectMapper;
import org.simpleframework.http.Path;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static java.util.Map.Entry;

/**
 * Basic server using Simple Framework.
 */
public class SimpleServer implements Container {

    public static class Task implements Runnable {

        private final Response response;
        private final Request request;

        public Task(Request request, Response response) {
            this.response = response;
            this.request = request;
        }

        public void run() {
            try {
                PrintStream body = response.getPrintStream();
                Map<String, String> map = new HashMap<String, String>();
                for (Entry<String, String> entry : request.getQuery().entrySet()) {
                    map.put(entry.getKey(), entry.getValue());
                }

                MeasurementsAPI.JsonResponse jsonResponse = MeasurementsAPI.getResponse(map);
                if (jsonResponse instanceof MeasurementsAPI.PaginatedResults) {
                    MeasurementsAPI.PaginatedResults paginatedResults =
                            (MeasurementsAPI.PaginatedResults) jsonResponse;

                    // This doesn't include the protocol, hostname,
                    // or port; only the path + query.
                    Path path = request.getPath();
                    String prefix = path.toString();
                    if (paginatedResults.previous != null) {
                        paginatedResults.previous = prefix + paginatedResults.previous;
                    }
                    if (paginatedResults.next != null) {
                        paginatedResults.next = prefix + paginatedResults.next;
                    }

                    jsonResponse = paginatedResults;
                }

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

    public SimpleServer(int size) {
        this.executor = Executors.newFixedThreadPool(size);
    }

    public void handle(Request request, Response response) {
        Task task = new Task(request, response);
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