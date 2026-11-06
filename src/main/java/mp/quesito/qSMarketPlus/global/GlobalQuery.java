package mp.quesito.qSMarketPlus.global;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.database.SQLManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GlobalQuery {

    public record ShopRow(String playerName, String type, String itemName, int amount, double total, long timestamp) {}
    public record TradeRow(UUID playerA, UUID playerB, double moneyA, double moneyB, double xpA, double xpB, long timestamp) {}
    public record AuctionLogRow(String type, String sellerName, String buyerName, String itemName, double price, long timestamp) {}

    public static long sinceMs(int timeIndex) {
        long now = System.currentTimeMillis();
        return switch (timeIndex) {
            case 1 -> now - 24L * 3600_000L;
            case 2 -> now - 7L * 24 * 3600_000L;
            case 3 -> now - 30L * 24 * 3600_000L;
            default -> 0L;
        };
    }

    // ==============================
    //   TIENDA (shop_transactions)
    // ==============================
    public static List<ShopRow> shop(int offset, int limit, UUID player, int timeIndex) {
        List<ShopRow> rows = new ArrayList<>();
        List<String> where = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        long since = sinceMs(timeIndex);

        if (player != null) {
            where.add("player_uuid = ?");
            params.add(player.toString());
        }
        if (since > 0) {
            where.add("timestamp >= ?");
            params.add(since);
        }

        String sql = "SELECT player_name, type, item_name, amount, total, timestamp FROM shop_transactions";
        if (!where.isEmpty()) sql += " WHERE " + String.join(" AND ", where);
        sql += " ORDER BY timestamp DESC LIMIT ? OFFSET ?";
        params.add(limit);
        params.add(offset);

        sql().query(sql, rs -> {
            try {
                while (rs.next()) {
                    String itemName = rs.getString("item_name");
                    if (itemName == null || itemName.isEmpty()) itemName = "?";
                    rows.add(new ShopRow(
                            rs.getString("player_name"),
                            rs.getString("type"),
                            itemName,
                            rs.getInt("amount"),
                            rs.getDouble("total"),
                            rs.getLong("timestamp")
                    ));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, params.toArray());

        return rows;
    }

    // ==============================
    //   TRADEOS (trade_history)
    // ==============================
    public static List<TradeRow> trades(int offset, int limit, UUID player, int timeIndex) {
        List<TradeRow> rows = new ArrayList<>();
        List<String> where = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        long since = sinceMs(timeIndex);

        if (player != null) {
            where.add("(player_a = ? OR player_b = ?)");
            params.add(player.toString());
            params.add(player.toString());
        }
        if (since > 0) {
            where.add("timestamp >= ?");
            params.add(since);
        }

        String sql = "SELECT player_a, player_b, money_a, money_b, xp_a, xp_b, timestamp FROM trade_history";
        if (!where.isEmpty()) sql += " WHERE " + String.join(" AND ", where);
        sql += " ORDER BY timestamp DESC LIMIT ? OFFSET ?";
        params.add(limit);
        params.add(offset);

        sql().query(sql, rs -> {
            try {
                while (rs.next()) {
                    rows.add(new TradeRow(
                            UUID.fromString(rs.getString("player_a")),
                            UUID.fromString(rs.getString("player_b")),
                            rs.getDouble("money_a"),
                            rs.getDouble("money_b"),
                            rs.getDouble("xp_a"),
                            rs.getDouble("xp_b"),
                            rs.getLong("timestamp")
                    ));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, params.toArray());

        return rows;
    }

    // ==============================
    //   SUBASTAS (auction_log)
    // ==============================
    public static List<AuctionLogRow> auctionLog(int offset, int limit, UUID player, int timeIndex) {
        List<AuctionLogRow> rows = new ArrayList<>();
        List<String> where = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        long since = sinceMs(timeIndex);

        if (player != null) {
            where.add("(seller_uuid = ? OR buyer_uuid = ?)");
            params.add(player.toString());
            params.add(player.toString());
        }
        if (since > 0) {
            where.add("created >= ?");
            params.add(since);
        }

        String sql = "SELECT type, seller_name, buyer_name, item_name, price, created FROM auction_log";
        if (!where.isEmpty()) sql += " WHERE " + String.join(" AND ", where);
        sql += " ORDER BY created DESC LIMIT ? OFFSET ?";
        params.add(limit);
        params.add(offset);

        sql().query(sql, rs -> {
            try {
                while (rs.next()) {
                    String itemName = rs.getString("item_name");
                    if (itemName == null || itemName.isEmpty()) itemName = "?";
                    rows.add(new AuctionLogRow(
                            rs.getString("type"),
                            rs.getString("seller_name"),
                            rs.getString("buyer_name"),
                            itemName,
                            rs.getDouble("price"),
                            rs.getLong("created")
                    ));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, params.toArray());

        return rows;
    }

    private static SQLManager sql() {
        return QSMarketPlus.getInstance().getSqlManager();
    }
}