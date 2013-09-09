import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;

/**
 * Utility class for setting up the database connection pool.
 */
@SuppressWarnings("SpellCheckingInspection")
class Database {

    public static class ConnectionDetails {
        public String username;
        public String password;
        public String jdbcUrl;
    }

    private static ConnectionDetails getConnectionDetails()
            throws URISyntaxException {
        URI dbUri = new URI(System.getenv("DATABASE_URL"));

        String username = dbUri.getUserInfo().split(":")[0];
        String password;
        try {
            password = dbUri.getUserInfo().split(":")[1];
        } catch (Exception e) {
            password = "";
        }
        String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();

        ConnectionDetails connectionDetails = new ConnectionDetails();
        connectionDetails.username = username;
        connectionDetails.password = password;
        connectionDetails.jdbcUrl = dbUrl;

        return connectionDetails;
    }

    public static BoneCP getConnectionPool() throws SQLException, URISyntaxException {
        ConnectionDetails connectionDetails = getConnectionDetails();

        BoneCPConfig config = new BoneCPConfig();
        config.setJdbcUrl(connectionDetails.jdbcUrl);
        config.setUsername(connectionDetails.username);
        config.setPassword(connectionDetails.password);
        config.setMinConnectionsPerPartition(5);
        config.setMaxConnectionsPerPartition(10);
        config.setPartitionCount(1);

        return new BoneCP(config);
    }
}
