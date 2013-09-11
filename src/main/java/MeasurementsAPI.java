import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgis.PGgeometry;
import org.postgis.Point;

import java.net.URISyntaxException;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class MeasurementsAPI {

    private static final String COUNT_QUERY = "SELECT COUNT(*) FROM observations_measurement";
    private static final String COMPACT_QUERY = "SELECT id, location FROM observations_measurement ORDER BY reference_timestamp DESC";
    private static final String FULL_QUERY = "SELECT id, location, created_timestamp, reference_timestamp, location_reference FROM observations_measurement ORDER BY reference_timestamp DESC";

    public static class Properties {
        public final long id;

        public Properties(long id) {
            this.id = id;
        }
    }

    public static class FullProperties extends Properties {
        public final Date created_timestamp;
        public final Date reference_timestamp;
        public final String location_reference;

        public FullProperties(long id, Date created, Date date, String location) {
            super(id);
            this.created_timestamp = created;
            this.reference_timestamp = date;
            this.location_reference = location;
        }
    }

    public static class Geometry {
        public final String type = "Point";
        public final double[] coordinates;

        public Geometry(Point point) {
            this.coordinates = new double[2];
            this.coordinates[0] = point.x;
            this.coordinates[1] = point.y;
        }
    }

    public static class Feature {
        public final String type = "Feature";
        public Properties properties;
        public Geometry geometry;
    }

    public static class JsonResponse {
        //
    }

    public static class PaginatedResults extends JsonResponse {
        public long count;
        public String next;
        public String previous;
        public List<Feature> results;
    }

    public static class FullResults extends JsonResponse {
        public final String type = "FeatureCollection";
        public List<Feature> features;
    }

    /**
     * Helper for getting the total number of rows either from the db or from the cache.
     *
     * @return The total number of measurements.
     * @throws ExecutionException
     */
    private static Long getCount() throws ExecutionException {
        CacheManager cm = CacheManager.getInstance();
        return (Long) cm.cache.get("total-measurements", new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                Connection connection = null;
                Long dbCount = Long.valueOf(0);
                ResultSet rset = null;
                Statement stmt = null;
                try {
                    connection = DatabaseManager.getConnection();
                    stmt = connection.createStatement();
                    rset = stmt.executeQuery(COUNT_QUERY);
                    rset.next();
                    dbCount = rset.getLong(1);
                } catch(SQLException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (rset != null) {
                            rset.close();
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    if (stmt != null) {
                        try {
                            stmt.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                    if (connection != null) {
                        try {
                            connection.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return dbCount;
            }
        });
    }

    /**
     * Given the request parameters as a `Map`, we build the response,
     * including the appropriate measurements + metadata.
     *
     * @param params Parsed from the query string
     * @return The response object to be serialized as JSON
     * @throws ExecutionException
     */
    public static <T extends JsonResponse> T getResponse(Map<String,
            String> params)
            throws ExecutionException {
        // If `page` and `page_size` weren't present in the request
        // parameters, we insert them for the the previous / next links
        if (!params.containsKey("page")) {
            params.put("page", String.valueOf(1));
        }
        if (!params.containsKey("page_size")) {
            params.put("page_size", String.valueOf(10));
        }

        boolean compact = Boolean.parseBoolean(params.get("compact"));
        int page = Integer.parseInt(params.get("page"));
        int pageSize = Integer.parseInt(params.get("page_size"));

        if (pageSize > 0) {
            PaginatedResults response = new PaginatedResults();
            response.results = getFeatures(compact, page, pageSize);
            response.count = getCount();

            int lastPage = 1;
            if (pageSize > 0) {
                lastPage = (int) (response.count / pageSize) + 1;
            }
            StringBuffer link = new StringBuffer("?");

            for (String key : params.keySet()) {
                link.append(key).append("=").append(params.get(key)).append("&");
            }
            if (page != 1) {
                response.previous = link.toString().replaceAll("&$", "")
                        .replaceAll("page=[0-9]+", "page=" + (page - 1));
            }

            if (page != lastPage) {
                response.next = link.toString().replaceAll("&$", "")
                        .replaceAll("page=[0-9]+", "page=" + (page + 1));
            }
            return (T) response;
        } else {
            FullResults response = new FullResults();
            response.features = getFeatures(compact, page, pageSize);
            return (T) response;
        }
    }

    private static List<Feature> getFeatures(boolean compact, int page, int pageSize) {
        // This may mean we create an ArrayList that's larger than we need,
        // but I'm OK with that
        ArrayList<Feature> features = new ArrayList<Feature>(pageSize);
        PreparedStatement stmt = null;
        ResultSet rset = null;
        Connection connection = null;

        int offset = (page - 1) * pageSize;

        try {
            connection = DatabaseManager.getConnection();

            String query;
            if (compact) {
                query = COMPACT_QUERY;
            } else {
                query = FULL_QUERY;
            }

            if (pageSize > 0) {
                query += " LIMIT ? OFFSET ?";
                stmt = connection.prepareStatement(query);
                stmt.setInt(1, pageSize);
                stmt.setInt(2, offset);
            } else {
                stmt = connection.prepareStatement(query);
            }

            rset = stmt.executeQuery();
            while (rset.next()) {
                long id = rset.getLong("id");
                PGgeometry geom = (PGgeometry) rset.getObject("location");
                Point point = new Point(geom.getValue());

                Feature feature = new Feature();
                if (compact) {
                    feature.properties = new Properties(id);
                } else {
                    feature.properties = new FullProperties(
                            id,
                            rset.getTimestamp("created_timestamp"),
                            rset.getTimestamp("reference_timestamp"),
                            rset.getString("location_reference")
                    );
                }
                feature.geometry = new Geometry(point);
                features.add(feature);
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } finally {
            if (rset != null) {
                try {
                    rset.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return features;
    }

    public static ObjectMapper getMapper() {
        ObjectMapper mapper = new ObjectMapper();
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(tz);
        mapper.setDateFormat(df);
        return mapper;
    }
}
