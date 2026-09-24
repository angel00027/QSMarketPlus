package mp.quesito.qSMarketPlus.auction;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import mp.quesito.qSMarketPlus.database.SQLManager;
import mp.quesito.qSMarketPlus.utils.ItemSerializer;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.util.*;

public class AuctionDAO {

    private final SQLManager sql;
    private final Gson gson = new Gson();

    public AuctionDAO(SQLManager sql) {
        this.sql = sql;
    }

    // ==============================
    //   PARSEAR FILA (tolerante)
    // ==============================
    private AuctionItem readRow(ResultSet rs) {
        try {
            int id = rs.getInt("id");
            UUID seller = UUID.fromString(rs.getString("seller"));
            String buyerStr = rs.getString("buyer");
            UUID buyer = buyerStr == null || buyerStr.isEmpty() ? null : UUID.fromString(buyerStr);

            double price = rs.getDouble("price");
            long created = rs.getLong("created");
            long expires = rs.getLong("expires");
            String statusStr = rs.getString("status");

            String itemStr = rs.getString("item");
            String contStr = rs.getString("container");

            AuctionItem auction;

if (itemStr != null && !itemStr.isEmpty()) {
                ItemStack item = ItemSerializer.fromBase64(itemStr, true);
                auction = new AuctionItem(seller, item, price, created, expires);
            } else {
                ItemStack[] contents = containerFromJson(contStr);
                auction = new AuctionItem(seller, contents, price, created, expires);
            }

            auction.id = id;
            auction.buyer = buyer;
            auction.status = AuctionItem.Status.valueOf(statusStr.toUpperCase(Locale.ROOT));

            return auction;
        } catch (Exception e) {
            try {
                System.err.println("[AuctionDAO] Fila de subasta corrupta, omitida (id=" + rs.getInt("id") + "): " + e.getMessage());
            } catch (Exception ignored) { }
            return null;
        }
    }

    // ==============================
    //   CARGAR ACTIVAS
    // ==============================
    public List<AuctionItem> loadActiveAuctions() {
        List<AuctionItem> list = new ArrayList<>();
        sql.query("SELECT * FROM auctions WHERE status = 'ACTIVE'", rs -> {
            try {
                while (rs.next()) {
                    AuctionItem auction = readRow(rs);
                    if (auction != null) list.add(auction);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        return list;
    }

    // ==============================
    //   HISTORIAL POR JUGADOR
    // ==============================
    public List<AuctionItem> loadHistory(UUID sellerUuid, int limit, int offset) {
        List<AuctionItem> list = new ArrayList<>();
        sql.query("""
                    SELECT * FROM auctions
                    WHERE seller = ? AND status <> 'ACTIVE'
                    ORDER BY created DESC
                    LIMIT ? OFFSET ?
                  """,
                rs -> {
                    try {
                        while (rs.next()) {
                            AuctionItem auction = readRow(rs);
                            if (auction != null) list.add(auction);
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                },
                sellerUuid.toString(), limit, offset
        );
        return list;
    }

    public List<AuctionItem> loadExpired(UUID seller) {
        List<AuctionItem> list = new ArrayList<>();
        sql.query("""
            SELECT * FROM auctions
            WHERE seller = ? AND status = 'EXPIRED'
            ORDER BY expires DESC
        """,
                rs -> {
                    try {
                        while (rs.next()) {
                            AuctionItem auction = readRow(rs);
                            if (auction != null) list.add(auction);
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                },
                seller.toString()
        );
        return list;
    }

    public void markAsTaken(AuctionItem auction) {
        sql.update("""
            UPDATE auctions
            SET status = 'EXPIRED_TAKEN'
            WHERE id = ?
        """, auction.id);
    }

    // Eliminación administrativa (retirada definitiva de la fila)
    public boolean delete(int id) {
        return sql.update("DELETE FROM auctions WHERE id = ?", id) > 0;
    }

    // ==============================
    //   INSERTAR SUBASTA (devuelve éxito)
    // ==============================
    public boolean insertAuction(AuctionItem auction) {
        String sqlInsert = """
            INSERT INTO auctions
            (seller, buyer, price, created, expires, status, item, container)
            VALUES (?,?,?,?,?,?,?,?)
        """;

        try (Connection conn = this.sql.getConnection();
             PreparedStatement st = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {

            st.setString(1, auction.seller.toString());
            st.setString(2, auction.buyer == null ? null : auction.buyer.toString());
            st.setDouble(3, auction.price);
            st.setLong(4, auction.created);
            st.setLong(5, auction.expiresAt);
            st.setString(6, auction.status.name());

            if (!auction.isBulk()) {
                String b64 = ItemSerializer.toBase64(auction.item);
                if (b64 == null) {
                    System.err.println("[AuctionDAO] No se pudo serializar el item de " + auction.seller + "; subasta abortada.");
                    return false;
                }
                st.setString(7, b64);
                st.setNull(8, Types.LONGVARCHAR);
            } else {
                st.setNull(7, Types.LONGVARCHAR);
                st.setString(8, containerToJson(auction.container));
            }

            st.executeUpdate();

            try (ResultSet keys = st.getGeneratedKeys()) {
                if (keys.next()) {
                    auction.id = keys.getInt(1);
                }
            }

            return auction.id > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==============================
    //   ACTUALIZAR ESTADO + BUYER
    // ==============================
    public boolean updateStatusAndBuyer(AuctionItem auction) {
        if (auction.id <= 0) return false;

        return this.sql.update("""
                UPDATE auctions
                SET status = ?, buyer = ?
                WHERE id = ?
                """,
                auction.status.name(),
                auction.buyer == null ? null : auction.buyer.toString(),
                auction.id
        ) > 0;
    }

    // SOLO estado (ej: cancelado/expirado sin buyer)
    public boolean updateStatus(AuctionItem auction) {
        if (auction.id <= 0) return false;

        return this.sql.update("""
                UPDATE auctions
                SET status = ?
                WHERE id = ?
                """,
                auction.status.name(),
                auction.id
        ) > 0;
    }

    // ==============================
    //   AUCTION LOG (historial global)
    // ==============================
    public void log(AuctionItem auc, String type, String sellerName, String buyerName, String itemName) {
        String snapshot = null;
        try {
            if (!auc.isBulk()) {
                snapshot = ItemSerializer.toBase64(auc.item);
            } else {
                snapshot = containerToJson(auc.container);
            }
        } catch (Exception ignored) { }

        this.sql.update("""
                INSERT INTO auction_log
                (auction_id, type, seller_uuid, seller_name, buyer_uuid, buyer_name, item_name, price, created, item_base64)
                VALUES (?,?,?,?,?,?,?,?,?,?)
                """,
                auc.id,
                type,
                auc.seller == null ? null : auc.seller.toString(),
                sellerName,
                auc.buyer == null ? null : auc.buyer.toString(),
                buyerName,
                itemName != null && !itemName.isEmpty() ? itemName : "Desconocido",
                auc.price,
                auc.created,
                snapshot
        );
    }

    // ==============================
    //   SERIALIZACIÓN (base64 robusto)
    // ==============================
    // Solo guarda items reales (nada de JsonNull), evita filas que rompen el lote
    private String containerToJson(ItemStack[] contents) throws Exception {
        JsonArray arr = new JsonArray();

        for (ItemStack it : contents) {
            if (it == null || it.getType().isAir()) continue;
            String b64 = ItemSerializer.toBase64(it);
            if (b64 != null) arr.add(new com.google.gson.JsonPrimitive(b64));
        }

        return gson.toJson(arr);
    }

    // Lectura tolerante: cualquier base64 corroído se omite sin romper el resto
    private ItemStack[] containerFromJson(String json) {
        if (json == null || json.isEmpty()) return new ItemStack[0];

        try {
            JsonArray arr = new JsonParser().parse(json).getAsJsonArray();
            List<ItemStack> out = new ArrayList<>();

            for (int i = 0; i < arr.size(); i++) {
                try {
                    if (arr.get(i).isJsonNull()) continue;
                    String s = arr.get(i).getAsString();
                    if (s == null || s.isEmpty() || s.equals("null")) continue;

                    ItemStack item = ItemSerializer.fromBase64(s, true);
                    if (item != null && !item.getType().isAir()) out.add(item);
                } catch (Exception ignored) { }
            }

            return out.toArray(new ItemStack[0]);
        } catch (Exception e) {
            return new ItemStack[0];
        }
    }
}