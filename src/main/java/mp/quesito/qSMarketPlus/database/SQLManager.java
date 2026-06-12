package mp.quesito.qSMarketPlus.database;

import mp.quesito.qSMarketPlus.QSMarketPlus;

import java.io.File;
import java.sql.*;
import java.util.function.Consumer;

public class SQLManager {

    private final QSMarketPlus plugin;
    private IDatabase db;

    public SQLManager(QSMarketPlus plugin) {
        this.plugin = plugin;
    }

    public void init() {

        plugin.getLogger().info("Iniciando conexión SQL...");

        String type = plugin.getConfig().getString("database.type", "sqlite").toLowerCase();

        switch (type) {

            case "mysql" -> {
                String host = plugin.getConfig().getString("database.mysql.host");
                int port = plugin.getConfig().getInt("database.mysql.port");
                String database = plugin.getConfig().getString("database.mysql.database");
                String user = plugin.getConfig().getString("database.mysql.user");
                String pass = plugin.getConfig().getString("database.mysql.password");

                db = new MySQLDatabase(host, port, database, user, pass);
                plugin.getLogger().info("Base seleccionada: MySQL");
            }

            default -> {
                File f = new File(plugin.getDataFolder(),
                        plugin.getConfig().getString("database.sqlite.file", "data.db"));

                db = new SQLiteDatabase(f);
                plugin.getLogger().info("Base seleccionada: SQLite");
            }
        }

        createTables();
    }

    public Connection getConnection() throws SQLException {
        return db.getConnection();
    }

    public void shutdown() {
        if (db != null) db.shutdown();
    }

    // ===============================================================
    //                     MÉTODOS QUERY / UPDATE
    // ===============================================================

    public void query(String sql, Consumer<ResultSet> callback, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                callback.accept(rs);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int update(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            return ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ===============================================================
    //                   TABLAS COMPATIBLES PARA AMBAS DB
    // ===============================================================
    private void createTables() {

        String sqlite = """
            CREATE TABLE IF NOT EXISTS auctions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                seller TEXT NOT NULL,
                buyer TEXT,
                price DOUBLE NOT NULL,
                created BIGINT NOT NULL,
                expires BIGINT NOT NULL,
                status TEXT NOT NULL,
                item TEXT,
                container TEXT
            );
        """;

        String mysql = """
            CREATE TABLE IF NOT EXISTS auctions (
                id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                seller VARCHAR(64) NOT NULL,
                buyer VARCHAR(64),
                price DOUBLE NOT NULL,
                created BIGINT NOT NULL,
                expires BIGINT NOT NULL,
                status VARCHAR(32) NOT NULL,
                item LONGTEXT,
                container LONGTEXT
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """;

        String sqliteShops = """
            CREATE TABLE IF NOT EXISTS player_shops (
                world TEXT NOT NULL,
                x INTEGER NOT NULL,
                y INTEGER NOT NULL,
                z INTEGER NOT NULL,
                sign_x INTEGER,
                sign_y INTEGER,
                sign_z INTEGER,
                owner_uuid TEXT NOT NULL,
                price DOUBLE NOT NULL,
                amount_per_sale INTEGER NOT NULL,
                items TEXT,
                created BIGINT NOT NULL,
                active INTEGER NOT NULL DEFAULT 1, -- 1 = activa, 0 = inactiva
                PRIMARY KEY (world, x, y, z)
            );


        """;

        String mysqlShops = """
            CREATE TABLE IF NOT EXISTS player_shops (
                world VARCHAR(50) NOT NULL,
                x INT NOT NULL,
                y INT NOT NULL,
                z INT NOT NULL,
                sign_x INT,
                sign_y INT,
                sign_z INT,
                owner_uuid VARCHAR(36) NOT NULL,
                price DOUBLE NOT NULL,
                amount_per_sale INT NOT NULL,
                items LONGTEXT,
                created BIGINT NOT NULL,
                active TINYINT(1) NOT NULL DEFAULT 1, -- 1 = activa, 0 = inactiva
                PRIMARY KEY (world, x, y, z)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

            """;
        // ======================= SQLite =======================
                String sqliteSignShops = """
        CREATE TABLE IF NOT EXISTS sign_shops (
            world TEXT NOT NULL,
            x INTEGER NOT NULL,
            y INTEGER NOT NULL,
            z INTEGER NOT NULL,
            owner_uuid TEXT NOT NULL,
            item_category TEXT DEFAULT 'none',
            item_id TEXT NOT NULL,
            buy_price REAL NOT NULL,
            sell_price REAL NOT NULL,
            amount INTEGER NOT NULL,
            created INTEGER NOT NULL,
            active INTEGER NOT NULL DEFAULT 1,
            PRIMARY KEY (world, x, y, z)
        );
        """;


        // ======================= MySQL =======================
                String mysqlSignShops = """
        CREATE TABLE IF NOT EXISTS sign_shops (
            world VARCHAR(50) NOT NULL,
            x INT NOT NULL,
            y INT NOT NULL,
            z INT NOT NULL,
            owner_uuid VARCHAR(36) NOT NULL,
            item_category VARCHAR(64) DEFAULT 'none',
            item_id VARCHAR(64) NOT NULL,
            buy_price DECIMAL(10,2) NOT NULL,
            sell_price DECIMAL(10,2) NOT NULL,
            amount INT NOT NULL,
            created BIGINT NOT NULL,
            active TINYINT(1) NOT NULL DEFAULT 1,
            PRIMARY KEY (world, x, y, z)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """;
        // ======================= Unique Purchases =======================
                String sqliteUnique = """
            CREATE TABLE IF NOT EXISTS unique_purchases (
                player_uuid TEXT NOT NULL,
                item_id TEXT NOT NULL,
                PRIMARY KEY(player_uuid, item_id)
            );
        """;

                String mysqlUnique = """
            CREATE TABLE IF NOT EXISTS unique_purchases (
                player_uuid VARCHAR(36) NOT NULL,
                item_id VARCHAR(64) NOT NULL,
                PRIMARY KEY(player_uuid, item_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """;

        if (db instanceof MySQLDatabase) {
            update(mysqlShops);
        } else {
            update(sqliteShops);
        }


        if (db instanceof MySQLDatabase) {
            update(mysql);
        } else {
            update(sqlite);
        }


        // Ejecutar creación de tabla
        if (db instanceof MySQLDatabase) {
            update(mysqlSignShops);
        } else {
            update(sqliteSignShops);
        }

        // Ejecutar tabla de compras únicas primero
        if (db instanceof MySQLDatabase) {
            update(mysqlUnique);
        } else {
            update(sqliteUnique);
        }
    }

    // Dentro de SQLManager
    public boolean isMySQL() {
        return db instanceof MySQLDatabase;
    }
}
