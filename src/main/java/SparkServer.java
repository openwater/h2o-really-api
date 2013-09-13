import com.fasterxml.jackson.databind.ObjectMapper;
import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static spark.Spark.*;

/**
 * Basic server using the Spark micro-framework.
 */
public class SparkServer {

    public static void main(String[] args) {

        setPort(Integer.valueOf(System.getenv("PORT")));

        get(new Route("/") {
            @Override
            public Object handle(Request request, Response response) {
                response.type("application/json");
                return null;
            }
        });

        // I have no idea why this can't happen inside `get`...
        after(new Filter("/") {
            @Override
            public void handle(Request request, Response response) {
                Map<String, String[]> params = request.queryMap().toMap();
                Map<String, String> singleParams = new HashMap<String, String>(params.size());
                for (String key : params.keySet()) {
                    singleParams.put(key, params.get(key)[0]);
                }

                MeasurementsAPI.JsonResponse jsonResponse = null;
                try {
                    jsonResponse = MeasurementsAPI.getResponse(singleParams);
                    if (jsonResponse instanceof MeasurementsAPI.PaginatedResults) {
                        MeasurementsAPI.PaginatedResults paginatedResults =
                                (MeasurementsAPI.PaginatedResults) jsonResponse;

                        if (paginatedResults.previous != null) {
                            paginatedResults.previous = request.url() + paginatedResults.previous;
                        }
                        if (paginatedResults.next != null) {
                            paginatedResults.next = request.url() + paginatedResults.next;
                        }

                        jsonResponse = paginatedResults;
                    }
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

                try {
                    ObjectMapper mapper = MeasurementsAPI.getMapper();
                    response.body(mapper.writeValueAsString(jsonResponse));
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        });
    }
}
