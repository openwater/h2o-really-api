import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.IOException;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Basic server using Undertow.
 */
public class UndertowServer {

    public static void main(final String[] args) {
        Undertow server = Undertow.builder()
                .addListener(Integer.valueOf(System.getenv("PORT")), "localhost")
                .setHandler(new HttpHandler() {
                    @Override
                    public void handleRequest(final HttpServerExchange exchange) throws Exception {
                        Map<String, Deque<String>> params = exchange.getQueryParameters();
                        Map<String, String> singleParams = new HashMap<String, String>(params.size());
                        for (String key : params.keySet()) {
                            singleParams.put(key, params.get(key).getFirst());
                        }

                        MeasurementsAPI.JsonResponse jsonResponse = null;
                        try {
                            jsonResponse = MeasurementsAPI.getResponse(singleParams);
                            if (jsonResponse instanceof MeasurementsAPI.PaginatedResults) {
                                MeasurementsAPI.PaginatedResults paginatedResults =
                                        (MeasurementsAPI.PaginatedResults) jsonResponse;

                                if (paginatedResults.previous != null) {
                                    paginatedResults.previous = exchange.getRequestURL() + paginatedResults.previous;
                                }
                                if (paginatedResults.next != null) {
                                    paginatedResults.next = exchange.getRequestURL() + paginatedResults.next;
                                }

                                jsonResponse = paginatedResults;
                            }
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }

                        try {
                            ObjectMapper mapper = MeasurementsAPI.getMapper();
                            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                            exchange.getResponseSender().send(mapper.writeValueAsString(jsonResponse));
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                    }
                }).build();
        server.start();
    }
}
