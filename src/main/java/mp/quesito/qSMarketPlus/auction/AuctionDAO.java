package mp.quesito.qSMarketPlus.auction;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import mp.quesito.qSMarketPlus.database.SQLManager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Base64;

public class AuctionDAO {

    private final SQLManager sql;
    private final Gson gson = new Gson();

    public AuctionDAO(SQLManager sql) {
        this.sql = sql;
    }

    // ==============================
    //   CARGAR ACTIVAS
    // ==============================
    public List<AuctionItem> loadActiveAuctions() {

        List<AuctionItem> list = new ArrayList<>();

        sql.query("SELECT * FROM auctions WHERE status = 'ACTIVE'", rs -> {
            try {
                while (rs.next()) {

                    int id = rs.getInt("id");
                    UUID seller = UUID.fromString(rs.getString("seller"));
                    String buyerStr = rs.getString("buyer");
                    UUID buyer = buyerStr == null ? null : UUID.fromString(buyerStr);

                    double price = rs.getDouble("price");
                    long created = rs.getLong("created");
                    long expires = rs.getLong("expires");
                    String statusStr = rs.getString("status");

                    String itemStr = rs.getString("item");
                    String contStr = rs.getString("container");

                    AuctionItem auction;

                    if (itemStr != null && !itemStr.isEmpty()) {

                        ItemStack item = itemFromBase64(itemStr);
                        auction = new AuctionItem(seller, item, price, created, expires);

                    } else {

                        ItemStack[] contents = containerFromJson(contStr);
                        auction = new AuctionItem(seller, contents, price, created, expires);
                    }

                    auction.id = id;
                    auction.buyer = buyer;
                    auction.status = AuctionItem.Status.valueOf(statusStr.toUpperCase(Locale.ROOT));

                    list.add(auction);
                }
            } catch (Exception e) {
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

                            int id = rs.getInt("id");
                            UUID seller = UUID.fromString(rs.getString("seller"));
                            String buyerStr = rs.getString("buyer");
                            UUID buyer = buyerStr == null ? null : UUID.fromString(buyerStr);

                            double price = rs.getDouble("price");
                            long created = rs.getLong("created");
                            long expires = rs.getLong("expires");
                            String statusStr = rs.getString("status");

                            String itemStr = rs.getString("item");
                            String contStr = rs.getString("container");

                            AuctionItem auction;

                            if (itemStr != null && !itemStr.isEmpty()) {
                                ItemStack item = itemFromBase64(itemStr);
                                auction = new AuctionItem(seller, item, price, created, expires);
                            } else {
                                ItemStack[] contents = containerFromJson(contStr);
                                auction = new AuctionItem(seller, contents, price, created, expires);
                            }

                            auction.id = id;
                            auction.buyer = buyer;
                            auction.status = AuctionItem.Status.valueOf(statusStr.toUpperCase(Locale.ROOT));

                            list.add(auction);
                        }
                    } catch (Exception e) {
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

                            int id = rs.getInt("id");
                            UUID sellerId = UUID.fromString(rs.getString("seller"));

                            String buyerStr = rs.getString("buyer");
                            UUID buyer = buyerStr == null ? null : UUID.fromString(buyerStr);

                            double price = rs.getDouble("price");
                            long created = rs.getLong("created");
                            long expires = rs.getLong("expires");
                            String statusStr = rs.getString("status");

                            String itemStr = rs.getString("item");
                            String contStr = rs.getString("container");

                            AuctionItem auc;

                            if (itemStr != null && !itemStr.isEmpty()) {
                                auc = new AuctionItem(sellerId, itemFromBase64(itemStr), price, created, expires);
                            } else {
                                auc = new AuctionItem(sellerId, containerFromJson(contStr), price, created, expires);
                            }

                            auc.id = id;
                            auc.buyer = buyer;
                            auc.status = AuctionItem.Status.valueOf(statusStr);

                            list.add(auc);

                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
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


    // ==============================
    //   INSERTAR SUBASTA
    // ==============================
    public void insertAuction(AuctionItem auction) {

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
                st.setString(7, itemToBase64(auction.item));
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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==============================
    //   ACTUALIZAR ESTADO + BUYER
    // ==============================
    public void updateStatusAndBuyer(AuctionItem auction) {
        if (auction.id <= 0) return;

        this.sql.update("""
                UPDATE auctions
                SET status = ?, buyer = ?
                WHERE id = ?
                """,
                auction.status.name(),
                auction.buyer == null ? null : auction.buyer.toString(),
                auction.id
        );
    }

    // SOLO estado (ej: cancelado/expirado sin buyer)
    public void updateStatus(AuctionItem auction) {
        if (auction.id <= 0) return;

        this.sql.update("""
                UPDATE auctions
                SET status = ?
                WHERE id = ?
                """,
                auction.status.name(),
                auction.id
        );
    }

    // ==============================
    //   SERIALIZACIÓN
    // ==============================
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

    private String containerToJson(ItemStack[] contents) throws IOException {

        JsonArray arr = new JsonArray();

        for (ItemStack it : contents) {
            if (it == null) {
                // En lugar de meter el texto "null", metemos un valor nulo real de JSON
                arr.add(com.google.gson.JsonNull.INSTANCE);
            } else {
                // Envolvemos el Base64 en una primitiva de Gson para que sea un JsonElement válido
                arr.add(new com.google.gson.JsonPrimitive(itemToBase64(it)));
            }
        }

        return gson.toJson(arr);
    }

    private ItemStack[] containerFromJson(String json) throws IOException, ClassNotFoundException {
        if (json == null || json.isEmpty()) return new ItemStack[0];

        JsonArray arr = new JsonParser().parse(json).getAsJsonArray();

        ItemStack[] out = new ItemStack[arr.size()];

        for (int i = 0; i < arr.size(); i++) {
            String s = arr.get(i).getAsString();
            out[i] = s.equals("null") ? null : itemFromBase64(s);
        }

        return out;
    }


}
