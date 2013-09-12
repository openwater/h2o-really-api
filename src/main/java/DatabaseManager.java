import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Utility class for setting up the database connection pool.
 */
@SuppressWarnings("SpellCheckingInspection")
class DatabaseManager {

    private static DatabaseManager instance;
    public BoneCP connectionPool;

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

    /**
     * Helper to shortcut the verbose chain of method calls.
     *
     * @return Connection
     * @throws SQLException
     * @throws URISyntaxException
     */
    public static Connection getConnection() throws SQLException, URISyntaxException {
        return getInstance().connectionPool.getConnection();
    }

    protected DatabaseManager() {
        //
    }

    public static synchronized DatabaseManager getInstance() throws SQLException, URISyntaxException {
        if (instance == null) {
            instance = new DatabaseManager();

            ConnectionDetails connectionDetails = getConnectionDetails();
            BoneCPConfig config = new BoneCPConfig();
            config.setJdbcUrl(connectionDetails.jdbcUrl);
            config.setUsername(connectionDetails.username);
            config.setPassword(connectionDetails.password);
            config.setMinConnectionsPerPartition(5);
            config.setMaxConnectionsPerPartition(10);
            config.setPartitionCount(1);

            instance.connectionPool = new BoneCP(config);
        }
        return instance;
    }
}
