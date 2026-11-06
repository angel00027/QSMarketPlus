package mp.quesito.qSMarketPlus.stats;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.shop.ShopCategory;
import mp.quesito.qSMarketPlus.shop.ShopItem;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StatsManager {

    private final QSMarketPlus plugin;

    public StatsManager(QSMarketPlus plugin) {
        this.plugin = plugin;
    }

    public void recordBuy(Player player, ShopCategory category, ShopItem item, int amount, double total) {
        record(player, "BUY", category, item, amount, total);
    }

    public void recordSell(Player player, ShopCategory category, ShopItem item, int amount, double total) {
        record(player, "SELL", category, item, amount, total);
    }

    private void record(Player player, String type, ShopCategory category, ShopItem item, int amount, double total) {
        if (!plugin.getConfig().getBoolean("stats.enabled", true)) return;
        if (player == null || item == null || amount <= 0) return;
        try {
            String categoryId = category != null ? category.getId() : "-";
            String itemId = item.getId() == null ? "-" : item.getId();
            String itemName = item.getName() == null ? itemId : item.getName();
            String economy = item.getEconomy() == null ? "vault" : item.getEconomy();
            plugin.getSqlManager().update("""
                INSERT INTO shop_transactions
                (player_uuid, player_name, type, category_id, item_id, item_name, amount, total, economy, timestamp)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                """, player.getUniqueId().toString(), player.getName(), type,
                categoryId, itemId, itemName, amount, total, economy, System.currentTimeMillis());
        } catch (Exception e) {
            plugin.getLogger().warning("No se pudo registrar estadística: " + e.getMessage());
        }
    }

    public int purgeAll() {
        return plugin.getSqlManager().update("DELETE FROM shop_transactions");
    }

    public int purgePlayer(UUID uuid) {
        return plugin.getSqlManager().update("DELETE FROM shop_transactions WHERE player_uuid = ?", uuid.toString());
    }

    public String lastKnownName(UUID uuid) {
        final String[] name = {null};
        plugin.getSqlManager().query("""
                SELECT player_name FROM shop_transactions
                WHERE player_uuid = ?
                ORDER BY id DESC LIMIT 1
                """, rs -> {
            try {
                if (rs.next()) name[0] = rs.getString("player_name");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, uuid.toString());
        return name[0];
    }

    public StatsSummary getSummary(UUID uuid) {
        StatsSummary summary = new StatsSummary();
        plugin.getSqlManager().query("""
                SELECT type, COUNT(*) c, COALESCE(SUM(total), 0) t, COALESCE(AVG(total), 0) a
                FROM shop_transactions
                WHERE player_uuid = ?
                GROUP BY type
                """, rs -> {
            try {
                while (rs.next()) {
                    String type = rs.getString("type");
                    long count = rs.getLong("c");
                    double total = rs.getDouble("t");
                    double average = rs.getDouble("a");
                    if ("BUY".equals(type)) {
                        summary.buyCount = count;
                        summary.buyTotal = total;
                        summary.buyAverage = average;
                    } else if ("SELL".equals(type)) {
                        summary.sellCount = count;
                        summary.sellTotal = total;
                        summary.sellAverage = average;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, uuid.toString());
        return summary;
    }

    public List<ItemStat> getItemStats(UUID uuid) {
        List<ItemStat> list = new ArrayList<>();
        plugin.getSqlManager().query("""
                SELECT type, category_id, item_id, item_name,
                       COUNT(*) c, COALESCE(SUM(amount), 0) amt,
                       COALESCE(SUM(total), 0) t, COALESCE(AVG(total), 0) a
                FROM shop_transactions
                WHERE player_uuid = ?
                GROUP BY type, category_id, item_id
                ORDER BY t DESC
                """, rs -> {
            try {
                while (rs.next()) {
                    list.add(new ItemStat(
                            rs.getString("type"),
                            rs.getString("category_id"),
                            rs.getString("item_id"),
                            rs.getString("item_name"),
                            rs.getLong("c"),
                            rs.getLong("amt"),
                            rs.getDouble("t"),
                            rs.getDouble("a")
                    ));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, uuid.toString());
        return list;
    }

    public static class StatsSummary {
        public long buyCount;
        public double buyTotal;
        public double buyAverage;
        public long sellCount;
        public double sellTotal;
        public double sellAverage;
    }

    public static class ItemStat {
        public final String type;
        public final String categoryId;
        public final String itemId;
        public final String itemName;
        public final long count;
        public final long totalAmount;
        public final double totalMoney;
        public final double averageMoney;

        public ItemStat(String type, String categoryId, String itemId, String itemName,
                        long count, long totalAmount, double totalMoney, double averageMoney) {
            this.type = type;
            this.categoryId = categoryId;
            this.itemId = itemId;
            this.itemName = itemName;
            this.count = count;
            this.totalAmount = totalAmount;
            this.totalMoney = totalMoney;
            this.averageMoney = averageMoney;
        }
    }
}