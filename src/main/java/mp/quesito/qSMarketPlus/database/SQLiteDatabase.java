package mp.quesito.qSMarketPlus.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLiteDatabase implements IDatabase {

    private final File file;

    public SQLiteDatabase(File file) {
        this.file = file;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
    }

    @Override
    public void shutdown() {
        // SQLite no requiere cierre manual
    }
}
