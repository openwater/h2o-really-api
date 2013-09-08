import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgis.PGgeometry;
import org.postgis.Point;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

@SuppressWarnings("SpellCheckingInspection")
public class MeasurementsAPI {

    private static final String COMPACT_QUERY = "SELECT id, location FROM observations_measurement ORDER BY reference_timestamp DESC LIMIT ? OFFSET ?";
    private static final String FULL_QUERY = "SELECT id, location, created_timestamp, reference_timestamp, location_reference FROM observations_measurement ORDER BY reference_timestamp DESC LIMIT ? OFFSET ?";

    public static class Properties {
        public long id;

        public Properties(long id) {
            this.id = id;
        }
    }

    public static class FullProperties extends Properties {
        public Date created_timestamp;
        public Date reference_timestamp;
        public String location_reference;

        public FullProperties(long id, Date created, Date date, String location) {
            super(id);
            this.created_timestamp = created;
            this.reference_timestamp = date;
            this.location_reference = location;
        }
    }

    public static class Geometry {
        public final String type = "Point";
        public double[] coordinates;

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
        public long count;
        public String next;
        public String previous;
        public List<Feature> results;
    }

    protected static List<Feature> getFeatures(Connection connection, boolean compact, int page, int pageSize) {
        ArrayList<Feature> features = new ArrayList<Feature>(pageSize);
        PreparedStatement stmt = null;
        ResultSet rset = null;

        if (page <= 0) {
            page = 1;
        }
        if (pageSize < 0) {
            pageSize = 0;
        }

        int offset = (page - 1) * pageSize;

        try {
            if (compact) {
                stmt = connection.prepareStatement(COMPACT_QUERY);
            } else {
                stmt = connection.prepareStatement(FULL_QUERY);
            }
            stmt.setInt(1, pageSize);
            stmt.setInt(2, offset);
            rset = stmt.executeQuery();
            while (rset.next()) {
                long id = rset.getLong("id");
                PGgeometry geom = (PGgeometry) rset.getObject("location");
                Point point = new Point(geom.getValue());

                Feature feature = new Feature();
                if (compact) {
                    feature.properties = new Properties(id);
                } else {
                    Date created_timestamp = rset.getTimestamp("created_timestamp");
                    Date reference_timestamp = rset.getTimestamp("reference_timestamp");
                    String location_reference = rset.getString("location_reference");
                    feature.properties = new FullProperties(
                            id, created_timestamp, reference_timestamp, location_reference
                    );
                }
                feature.geometry = new Geometry(point);

                features.add(feature);
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
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
