package mp.quesito.qSMarketPlus.manager;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.database.SQLManager;
import mp.quesito.qSMarketPlus.shop.ShopItem;
import org.bukkit.entity.Player;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UniquePurchaseManager {

    private final QSMarketPlus plugin;
    private final SQLManager sql;

    public UniquePurchaseManager(QSMarketPlus plugin) {
        this.plugin = plugin;
        this.sql = plugin.getSqlManager(); // Asegúrate de tener getter para SQLManager
    }

    /**
     * Comprueba si el jugador ya compró un ítem único
     */
    public boolean hasPurchased(Player player, ShopItem item) {
        final boolean[] purchased = {false};

        String sqlQuery = "SELECT 1 FROM unique_purchases WHERE player_uuid = ? AND item_id = ? LIMIT 1;";
        sql.query(sqlQuery, rs -> {
            try {
                purchased[0] = rs.next();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, player.getUniqueId().toString(), item.getId());

        return purchased[0];
    }

    /**
     * Marca que el jugador compró un ítem único
     */
    public void markPurchased(Player player, ShopItem item) {
        String sqlUpdate = """
            INSERT INTO unique_purchases(player_uuid, item_id)
            VALUES(?, ?)
            ON CONFLICT(player_uuid, item_id) DO NOTHING;
            """;

        // En MySQL ON CONFLICT se reemplaza por INSERT IGNORE
        if (sql.isMySQL()) {
            sqlUpdate = """
                INSERT IGNORE INTO unique_purchases(player_uuid, item_id)
                VALUES(?, ?);
            """;
        }

        sql.update(sqlUpdate, player.getUniqueId().toString(), item.getId());
    }
}