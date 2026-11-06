package mp.quesito.qSMarketPlus.trade;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import mp.quesito.qSMarketPlus.database.SQLManager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class TradeDAO {

    private final SQLManager sql;
    private final Gson gson = new Gson();

    public TradeDAO(SQLManager sql) {
        this.sql = sql;
    }

    // ==============================
    //   INSERTAR TRADEO COMPLETADO
    // ==============================
    public void insertTrade(TradeRecord record) {

        String sqlInsert = """
            INSERT INTO trade_history
            (player_a, player_b, items_a, items_b, money_a, money_b, xp_a, xp_b, timestamp)
            VALUES (?,?,?,?,?,?,?,?,?)
        """;

        try {
            sql.update(sqlInsert,
                    record.playerA.toString(),
                    record.playerB.toString(),
                    itemsToBase64(record.itemsA),
                    itemsToBase64(record.itemsB),
                    record.moneyA,
                    record.moneyB,
                    record.xpA,
                    record.xpB,
                    record.timestamp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==============================
    //   HISTORIAL POR JUGADOR
    // ==============================
    public List<TradeRecord> loadHistory(UUID playerUuid, int limit, int offset) {

        List<TradeRecord> list = new ArrayList<>();

        sql.query("""
                    SELECT * FROM trade_history
                    WHERE player_a = ? OR player_b = ?
                    ORDER BY timestamp DESC
                    LIMIT ? OFFSET ?
                  """,
                rs -> {
                    try {
                        while (rs.next()) {
                            UUID a = UUID.fromString(rs.getString("player_a"));
                            UUID b = UUID.fromString(rs.getString("player_b"));

                            TradeRecord rec = new TradeRecord(
                                    a, b,
                                    itemsFromBase64(rs.getString("items_a")),
                                    itemsFromBase64(rs.getString("items_b")),
                                    rs.getDouble("money_a"),
                                    rs.getDouble("money_b"),
                                    rs.getInt("xp_a"),
                                    rs.getInt("xp_b"),
                                    rs.getLong("timestamp"));

                            rec.id = rs.getLong("id");
                            list.add(rec);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                playerUuid.toString(), playerUuid.toString(), limit, offset
        );

        return list;
    }

// ==============================
    //   MANTENIMIENTO (purga del historial)
    // ==============================
    public int purgeAll() {
        return sql.update("DELETE FROM trade_history");
    }

    public int purgePlayer(UUID playerUuid) {
        return sql.update("DELETE FROM trade_history WHERE player_a = ? OR player_b = ?",
                playerUuid.toString(), playerUuid.toString());
    }

    public int count() {
        final int[] total = {0};
        sql.query("SELECT COUNT(*) c FROM trade_history", rs -> {
            try {
                if (rs.next()) total[0] = rs.getInt("c");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return total[0];
    }

    // ==============================
    //   SERIALIZACIÓN
    // ==============================
    private String itemsToBase64(List<ItemStack> items) throws IOException {
        JsonArray arr = new JsonArray();

        for (ItemStack it : items) {
            if (it == null || it.getType().isAir()) {
                continue;
            }
            arr.add(new com.google.gson.JsonPrimitive(itemToBase64(it)));
        }

        return gson.toJson(arr);
    }

    private List<ItemStack> itemsFromBase64(String json) throws IOException, ClassNotFoundException {
        List<ItemStack> list = new ArrayList<>();

        if (json == null || json.isEmpty()) return list;

        JsonArray arr = new JsonParser().parse(json).getAsJsonArray();

        for (int i = 0; i < arr.size(); i++) {
            String s = arr.get(i).getAsString();
            if (s != null && !s.equals("null")) {
                list.add(itemFromBase64(s));
            }
        }

        return list;
    }

    private String itemToBase64(ItemStack item) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

        dataOutput.writeObject(item);
        dataOutput.close();

        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    private ItemStack itemFromBase64(String base64) throws IOException, ClassNotFoundException {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
        return (ItemStack) dataInput.readObject();
    }
}