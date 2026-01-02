package mp.quesito.qSMarketPlus.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class MySQLDatabase implements IDatabase {

    private HikariDataSource dataSource;

    public MySQLDatabase(String host, int port, String db, String user, String pass) {

        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(
                "jdbc:mysql://" + host + ":" + port + "/" + db
                        + "?useSSL=false&autoReconnect=true&allowPublicKeyRetrieval=true"
        );

        config.setUsername(user);
        config.setPassword(pass);
        config.setMaximumPoolSize(10);
        config.setPoolName("QSMarketPlus-MySQL");

        this.dataSource = new HikariDataSource(config);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void shutdown() {
        if (dataSource != null) dataSource.close();
    }
}
