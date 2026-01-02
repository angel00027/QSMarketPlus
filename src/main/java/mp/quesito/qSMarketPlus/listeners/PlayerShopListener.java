package mp.quesito.qSMarketPlus.listeners;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.manager.PlayerShopManager;
import mp.quesito.qSMarketPlus.shop.PlayerShop;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class PlayerShopListener implements Listener {

    private final PlayerShopManager shopManager =
            QSMarketPlus.getInstance().getShopManager();

    public static final NamespacedKey SHOP_OWNER =
            new NamespacedKey(QSMarketPlus.getInstance(), "shop-owner");

    // Jugador → Cofre seleccionado
    private final Map<UUID, Block> pendingChest = new HashMap<>();

    // Bloqueos temporales
    private final Map<Location, UUID> lockedBlocks = new HashMap<>();

    // ===================== INTERACT =====================
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null) return;

        // ===================== VINCULACIÓN CON REDSTONE =====================
        if (player.getInventory().getItemInMainHand().getType() == Material.REDSTONE) {

            // ===== SELECCIONAR COFRE =====
            if (block.getState() instanceof Chest) {
                Location loc = block.getLocation();

                if (shopManager.getShopAtLocation(loc) != null) {
                    player.sendMessage(ChatColor.RED +
                            "Este cofre ya pertenece a una tienda.");
                    return;
                }

                if (lockedBlocks.containsKey(loc)
                        && !lockedBlocks.get(loc).equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED +
                            "Este cofre está siendo usado por otro jugador.");
                    return;
                }

                pendingChest.put(player.getUniqueId(), block);
                lockedBlocks.put(loc, player.getUniqueId());

                player.sendMessage(ChatColor.AQUA +
                        "Cofre seleccionado. Ahora haz click en el cartel.");
                event.setCancelled(true);
                return;
            }

            // ===== SELECCIONAR CARTEL =====
            if (block.getState() instanceof Sign sign) {

                Block chestBlock = pendingChest.get(player.getUniqueId());
                if (chestBlock == null) {
                    player.sendMessage(ChatColor.RED +
                            "Primero selecciona un cofre.");
                    return;
                }

                Location signLoc = block.getLocation();
                Location chestLoc = chestBlock.getLocation();

                PlayerShop existingShop = shopManager.getShopAtLocation(signLoc);
                if (existingShop != null && !existingShop.getOwner().equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED +
                            "Este cartel ya pertenece a la tienda de " +
                            Bukkit.getOfflinePlayer(existingShop.getOwner()).getName());
                    cleanup(player);
                    return; // Bloquea vinculación
                }

                // 🔐 Registrar dueño del cartel
                sign.getPersistentDataContainer().set(
                        SHOP_OWNER,
                        PersistentDataType.STRING,
                        player.getUniqueId().toString()
                );

                // ===== LEER CONFIG DEL CARTEL (opcional) =====
                double price = 1.0;
                int amount = 1;

                try {
                    String line1 = sign.getLine(1);
                    if (line1 != null && line1.toLowerCase().startsWith("sell")) {
                        String[] parts = line1.split(" ");
                        if (parts.length >= 2)
                            price = Double.parseDouble(parts[1]);
                        if (parts.length >= 3 && parts[2].startsWith("x"))
                            amount = Integer.parseInt(parts[2].substring(1));
                    }
                } catch (Exception ignored) {}

                PlayerShop shop = new PlayerShop(
                        player.getUniqueId(),
                        chestLoc,
                        signLoc,
                        amount,
                        price
                );

                shopManager.addShop(shop);

                // 📝 Actualizar cartel
                Inventory inv = ((Chest) chestBlock.getState()).getInventory();
                ItemStack firstItem = Arrays.stream(inv.getContents())
                        .filter(i -> i != null && i.getType() != Material.AIR)
                        .findFirst().orElse(null);

                int stock = firstItem != null
                        ? Arrays.stream(inv.getContents())
                        .filter(i -> i != null && i.getType() == firstItem.getType())
                        .mapToInt(ItemStack::getAmount).sum()
                        : 0;

                String itemName = firstItem == null ? ChatColor.RED + "Sin stock" : ChatColor.GOLD + shop.getItemName(firstItem);
                ChatColor stockColor = stock >= amount ? ChatColor.WHITE : ChatColor.RED;

                sign.setLine(0, ChatColor.GREEN + "Tienda");
                sign.setLine(1, ChatColor.AQUA + player.getName());
                sign.setLine(2, ChatColor.YELLOW + "$" + price + " x" + ChatColor.LIGHT_PURPLE + amount);
                sign.setLine(3, itemName + ChatColor.GRAY + " : " + stockColor + stock);
                sign.update();

                player.sendMessage(ChatColor.GREEN + "Tienda creada correctamente.");

                cleanup(player);
                event.setCancelled(true);
            }

        }

        // ===================== INTERACCIÓN NORMAL =====================
        PlayerShop shop = getShopFromBlock(block);
        if (shop == null) return;

        event.setCancelled(true);

        // ===== ELIMINAR SHOP (SHIFT + CLICK CARTEL) =====
        if (player.isSneaking() && block.getState() instanceof Sign) {
            if (!shop.getOwner().equals(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED +
                        "No puedes eliminar tiendas ajenas.");
                return;
            }

            shop.destroyShop(false);
            shopManager.removeShop(shop);
            player.sendMessage(ChatColor.RED +
                    "Tienda eliminada.");
            return;
        }

        // ===== DUEÑO ABRE COFRE =====
        if (shop.getOwner().equals(player.getUniqueId())) {
            Inventory inv = shop.getChestInventory();
            if (inv != null) player.openInventory(inv);
            return;
        }

        // ===== COMPRAR =====
        shop.buy(player);
    }

    // ===================== INVENTARIO =====================
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Chest chest)) return;

        PlayerShop shop =
                shopManager.getShopAtLocation(chest.getLocation());
        if (shop != null) shop.updateSign();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Chest chest)) return;

        PlayerShop shop =
                shopManager.getShopAtLocation(chest.getLocation());
        if (shop != null) shop.updateSign();
    }

    // ===================== ROMPER TIENDA =====================
    @EventHandler
    public void onShopBreak(BlockBreakEvent event) {
        PlayerShop shop =
                getShopFromBlock(event.getBlock());
        if (shop == null) return;

        if (!shop.getOwner().equals(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(
                    "§cNo puedes romper tiendas ajenas.");
            return;
        }

        shop.destroyShop(false);
        shopManager.removeShop(shop);
        event.getPlayer().sendMessage("§eTienda eliminada.");
    }

    // ===================== SALIR =====================
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cleanup(event.getPlayer());
    }

    // ===================== UTILIDADES =====================
    private void cleanup(Player player) {
        UUID uuid = player.getUniqueId();

        Block chest = pendingChest.remove(uuid);
        if (chest != null)
            lockedBlocks.remove(chest.getLocation());

        lockedBlocks.entrySet()
                .removeIf(e -> e.getValue().equals(uuid));
    }

    private PlayerShop getShopFromBlock(Block block) {
        PlayerShop shop =
                shopManager.getShopAtLocation(block.getLocation());
        if (shop != null) return shop;

        if (block.getState() instanceof Sign) {
            for (PlayerShop s : shopManager.getAllShops()) {
                if (s.getSignLocation().equals(block.getLocation()))
                    return s;
            }
        }
        return null;
    }
}
