package mp.quesito.qSMarketPlus.manager;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.shop.PlayerShop;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;

public class PlayerShopManager {
    private final QSMarketPlus plugin;
    // Mapa principal: clave = "world;x;y;z" del cofre
    private final Map<String, PlayerShop> shops = new HashMap<>();

    // Mapa de tiendas por dueño
    private final Map<UUID, List<PlayerShop>> shopsByOwner = new HashMap<>();

    // Flag para evitar escribir mientras cargamos
    private boolean loadingShops = false;


    public PlayerShopManager(QSMarketPlus plugin) {
        this.plugin = plugin;
    }



    // ===================== CREAR / AGREGAR =====================
    public void addShop(PlayerShop shop) {
        addShop(shop, true);
    }

    // =====================================================
    // 📊 OBTENER CANTIDAD DE TIENDAS ACTIVAS DE UN JUGADOR
    // =====================================================
    public int getShopCount(UUID owner) {
        List<PlayerShop> ownerShops = shopsByOwner.get(owner);
        return ownerShops == null ? 0 : ownerShops.size();
    }

    public void addShop(PlayerShop shop, boolean save) {
        String key = locKey(shop.getChestLocation());
        if (shops.containsKey(key)) {
            plugin.getLogger().warning("Ya existe una tienda en esta ubicación: " + key);
            return;
        }

        shops.put(key, shop);
        shopsByOwner.computeIfAbsent(shop.getOwner(), k -> new ArrayList<>()).add(shop);

        if (save && !loadingShops) saveShop(shop); // evita conflictos SQLite al cargar
        shop.updateSign();
    }

    // ===================== ELIMINAR / DESTRUIR =====================
    public void removeShop(PlayerShop shop) {
        shop.destroyShop(false);

        String key = locKey(shop.getChestLocation());
        shops.remove(key);

        List<PlayerShop> ownerShops = shopsByOwner.get(shop.getOwner());
        if (ownerShops != null) {
            ownerShops.remove(shop);
            if (ownerShops.isEmpty()) shopsByOwner.remove(shop.getOwner());
        }

        deleteShop(shop);
    }




    // ===================== OBTENER TIENDAS =====================
    public List<PlayerShop> getShops(UUID owner) {
        return shopsByOwner.getOrDefault(owner, Collections.emptyList());
    }

    public PlayerShop getShopAtLocation(Location loc) {
        return shops.get(locKey(loc));
    }
    public void removeAllHolograms() {
        getAllShops().forEach(shop -> {
            if (shop.getHologram() != null)
                shop.getHologram().remove();
        });
    }



    public Collection<PlayerShop> getAllShops() {
        return shops.values();
    }

    // ===================== SQL =====================
    public void saveShop(PlayerShop shop) {
        String sql = """
            INSERT OR REPLACE INTO player_shops
            (world, x, y, z, sign_x, sign_y, sign_z, owner_uuid, price, amount_per_sale, items, created)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        plugin.getSqlManager().update(sql,
                shop.getChestLocation().getWorld().getName(),
                shop.getChestLocation().getBlockX(),
                shop.getChestLocation().getBlockY(),
                shop.getChestLocation().getBlockZ(),
                shop.getSignLocation().getBlockX(),
                shop.getSignLocation().getBlockY(),
                shop.getSignLocation().getBlockZ(),
                shop.getOwner().toString(),
                shop.getPrice(),
                shop.getAmountPerSale(),
                shop.serializeItems(),
                System.currentTimeMillis()
        );
    }

    public void deleteShop(PlayerShop shop) {
        String sql = "DELETE FROM player_shops WHERE world = ? AND x = ? AND y = ? AND z = ?";
        plugin.getSqlManager().update(sql,
                shop.getChestLocation().getWorld().getName(),
                shop.getChestLocation().getBlockX(),
                shop.getChestLocation().getBlockY(),
                shop.getChestLocation().getBlockZ()
        );
    }



    // ===================== CARGAR TODAS LAS TIENDAS =====================
    public void loadAllShops() {
        loadingShops = true; // activa flag para no guardar durante la carga

        String sql = "SELECT * FROM player_shops";

        plugin.getSqlManager().query(sql, rs -> {
            try {
                while (rs.next()) {

                    final UUID owner = UUID.fromString(rs.getString("owner_uuid"));
                    final String worldName = rs.getString("world");

                    final int x = rs.getInt("x");
                    final int y = rs.getInt("y");
                    final int z = rs.getInt("z");

                    final int signX = rs.getInt("sign_x");
                    final int signY = rs.getInt("sign_y");
                    final int signZ = rs.getInt("sign_z");

                    final double price = rs.getDouble("price");
                    final int amountPerSale = rs.getInt("amount_per_sale");
                    final String itemsBase64 = rs.getString("items");

                    final World world = Bukkit.getWorld(worldName);
                    if (world == null) continue; // evita errores si el mundo no existe

                    final Location chestLoc = new Location(world, x, y, z);
                    final Location signLoc = new Location(world, signX, signY, signZ);

                    final PlayerShop shop = new PlayerShop(owner, chestLoc, signLoc, amountPerSale, price);
                    shop.setActive(true);

                    if (itemsBase64 != null && !itemsBase64.isEmpty()) {
                        shop.deserializeItems(itemsBase64);
                    }

                    // Agrega sin guardar en DB
                    addShop(shop, false);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error cargando tiendas: " + e.getMessage());
                e.printStackTrace();
            }
        });

        loadingShops = false; // finaliza flag de carga
    }

    // ===================== ACTUALIZAR TIENDA =====================
    public void updateShop(PlayerShop shop) {
        String sql = """
        UPDATE player_shops
        SET price = ?, amount_per_sale = ?, items = ?
        WHERE world = ? AND x = ? AND y = ? AND z = ?
        """;

        plugin.getSqlManager().update(sql,
                shop.getPrice(),
                shop.getAmountPerSale(),
                shop.serializeItems(),
                shop.getChestLocation().getWorld().getName(),
                shop.getChestLocation().getBlockX(),
                shop.getChestLocation().getBlockY(),
                shop.getChestLocation().getBlockZ()
        );
    }

    // ===================== UTIL =====================
    private String locKey(Location loc) {
        return loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
    }
}
