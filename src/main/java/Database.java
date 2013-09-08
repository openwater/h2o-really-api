import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class Database {

    public final Connection connection;

    public Database() throws SQLException, URISyntaxException, ClassNotFoundException {
        URI dbUri = new URI(System.getenv("DATABASE_URL"));

        String username = dbUri.getUserInfo().split(":")[0];
        String password;
        try {
            password = dbUri.getUserInfo().split(":")[1];
        } catch (Exception e) {
            password = "";
        }
        String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();

        connection = DriverManager.getConnection(dbUrl, username, password);
        ((org.postgresql.PGConnection) connection).addDataType("geometry", Class.forName("org.postgis.PGgeometry"));
        ((org.postgresql.PGConnection) connection).addDataType("box3d", Class.forName("org.postgis.PGbox3d"));
    }
}
