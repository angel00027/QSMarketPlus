package mp.quesito.qSMarketPlus.manager;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.shop.SignShop;
import mp.quesito.qSMarketPlus.shop.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.*;

public class SignShopManager {

    private final QSMarketPlus plugin = QSMarketPlus.getInstance();
    private final Map<Location, SignShop> shops = new HashMap<>();

    // ===================== MAPA DE TIENDAS =====================
    public void addShop(SignShop shop, String ownerUUID) {
        shops.put(shop.getSignLocation(), shop);
        shop.updateSign();
        saveShop(shop, ownerUUID);
    }

    public void removeShop(SignShop shop) {
        shops.remove(shop.getSignLocation());
        deleteShop(shop);
    }

    public SignShop getShopAt(Location loc) {
        return shops.get(loc);
    }

    public Collection<SignShop> getAllShops() {
        return shops.values();
    }

    // ===================== SQL =====================
    public void saveShop(SignShop shop, String ownerUUID) {
        long created = System.currentTimeMillis();
        String sql = """
            INSERT OR REPLACE INTO sign_shops
            (world, x, y, z, owner_uuid, item_id, buy_price, sell_price, amount, created, active)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        plugin.getSqlManager().update(sql,
                shop.getSignLocation().getWorld().getName(),
                shop.getSignLocation().getBlockX(),
                shop.getSignLocation().getBlockY(),
                shop.getSignLocation().getBlockZ(),
                ownerUUID,
                shop.getItem().getId(), // obtener ID mediante getter
                shop.getBuyPrice(),
                shop.getSellPrice(),
                shop.getAmount(),
                created,
                shop.isActive() ? 1 : 0
        );
    }

    public void deleteShop(SignShop shop) {
        String sql = "DELETE FROM sign_shops WHERE world = ? AND x = ? AND y = ? AND z = ?";
        plugin.getSqlManager().update(sql,
                shop.getSignLocation().getWorld().getName(),
                shop.getSignLocation().getBlockX(),
                shop.getSignLocation().getBlockY(),
                shop.getSignLocation().getBlockZ()
        );
    }

    public void loadAllShops() {
        String sql = "SELECT * FROM sign_shops";
        plugin.getSqlManager().query(sql, rs -> {
            try {
                while (rs.next()) {
                    String world = rs.getString("world");
                    int x = rs.getInt("x");
                    int y = rs.getInt("y");
                    int z = rs.getInt("z");
                    String itemId = rs.getString("item_id");
                    String ownerUUID = rs.getString("owner_uuid");
                    double buyPrice = rs.getDouble("buy_price");
                    double sellPrice = rs.getDouble("sell_price");
                    int amount = rs.getInt("amount");
                    int activeInt = rs.getInt("active");

                    Location loc = new Location(Bukkit.getWorld(world), x, y, z);

                    // Obtener item desde ItemManager
                    ShopItem item = plugin.getItemManager().getItemById(itemId);
                    if (item == null) {
                        plugin.getLogger().warning("SignShop en " + loc + " no se cargó: item no encontrado en YML (ID: " + itemId + ")");
                        continue;
                    }

                    // Crear SignShop usando precios guardados
                    SignShop shop = new SignShop(loc, item, buyPrice, sellPrice, amount, ownerUUID);
                    shop.setActive(activeInt == 1);

                    // Agregar al mapa sin volver a guardar en SQL
                    shops.put(loc, shop);
                    shop.updateSign();
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error cargando SignShops: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
