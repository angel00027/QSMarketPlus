package mp.quesito.qSMarketPlus.listeners;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.manager.PlayerShopManager;
import mp.quesito.qSMarketPlus.shop.PlayerShop;
import mp.quesito.qSMarketPlus.utils.Lang;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
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

    // Caras horizontales para verificar cofres dobles adyacentes
    private final BlockFace[] CARDINAL_FACES = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

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
                    MessageUtil.msg(player, Lang.get("shop.already-exists"));
                    return;
                }

                if (lockedBlocks.containsKey(loc)
                        && !lockedBlocks.get(loc).equals(player.getUniqueId())) {
                    MessageUtil.msg(player, Lang.get("shop.chest-locked"));
                    return;
                }

                pendingChest.put(player.getUniqueId(), block);
                lockedBlocks.put(loc, player.getUniqueId());

                MessageUtil.msg(player, Lang.get("shop.chest-selected"));
                event.setCancelled(true);
                return;
            }

            // ===== SELECCIONAR CARTEL =====
            if (block.getState() instanceof Sign sign) {

                Block chestBlock = pendingChest.get(player.getUniqueId());
                if (chestBlock == null) {
                    MessageUtil.msg(player, Lang.get("shop.select-chest-first"));
                    return;
                }

                Location signLoc = block.getLocation();
                Location chestLoc = chestBlock.getLocation();

                PlayerShop existingShop = shopManager.getShopAtLocation(signLoc);
                if (existingShop != null && !existingShop.getOwner().equals(player.getUniqueId())) {
                    String ownerName = Bukkit.getOfflinePlayer(existingShop.getOwner()).getName();
                    MessageUtil.msg(player, Lang.get("shop.cannot-link-sign"),
                            Placeholder.parsed("owner", ownerName != null ? ownerName : "Desconocido"));
                    cleanup(player);
                    return;
                }

                // =========================================================================
                // 🛑 VERIFICACIÓN DE LÍMITES DINÁMICOS POR NÚMERO
                // =========================================================================
                if (!player.hasPermission("qsmarket.shop.limit.bypass")) {
                    int currentShops = shopManager.getShopCount(player.getUniqueId());
                    int maxAllowed = 1;

                    for (org.bukkit.permissions.PermissionAttachmentInfo attachment : player.getEffectivePermissions()) {
                        String permission = attachment.getPermission().toLowerCase();

                        if (permission.startsWith("qsmarket.shop.limit.") && attachment.getValue()) {
                            try {
                                String numberPart = permission.substring("qsmarket.shop.limit.".length());
                                int parsedLimit = Integer.parseInt(numberPart);

                                if (parsedLimit > maxAllowed) {
                                    maxAllowed = parsedLimit;
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }

                    if (currentShops >= maxAllowed) {
                        MessageUtil.msg(player, Lang.get("shop.limit-reached"),
                                Placeholder.parsed("limit", String.valueOf(maxAllowed)));
                        cleanup(player);
                        event.setCancelled(true);
                        return;
                    }
                }
                // =========================================================================

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

                MessageUtil.msg(player, Lang.get("shop.created-successfully"));

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
            if (!shop.getOwner().equals(player.getUniqueId()) && !player.hasPermission("qsmarket.admin")) {
                MessageUtil.msg(player, Lang.get("shop.cannot-delete-others"));
                return;
            }

            shop.destroyShop(false);
            shopManager.removeShop(shop);

            if (!shop.getOwner().equals(player.getUniqueId())) {
                MessageUtil.msg(player, Lang.get("admin.deleted-by-admin"));
            } else {
                MessageUtil.msg(player, Lang.get("shop.deleted"));
            }
            return;
        }

        // ===== DUEÑO ABRE COFRE =====
        if (shop.getOwner().equals(player.getUniqueId())) {
            Inventory inv = shop.getChestInventory();
            if (inv != null) player.openInventory(inv);
            return;
        }

        // ===== GESTIÓN DE ADMINISTRADORES (INSPECCIONAR VS COMPRAR) =====
        if (player.hasPermission("qsmarket.admin")) {
            // Si el admin está agachado (Shift), inspecciona el contenido en vez de comprar
            if (player.isSneaking()) {
                Inventory inv = shop.getChestInventory();
                if (inv != null) {
                    player.openInventory(inv);
                    MessageUtil.msg(player, Lang.get("admin.inspecting-shop"),
                            Placeholder.parsed("owner", shop.getOwnerName()));
                }
                return;
            }
        }

        // ===== COMPRAR =====
        shop.buy(player);
    }

    // ===================== 🛡️ EVITAR COFRES DOBLES DESTRUCTIVOS =====================
    @EventHandler
    public void onChestPlace(BlockPlaceEvent event) {
        Block placedBlock = event.getBlockPlaced();

        if (placedBlock.getType() != Material.CHEST) return;

        for (BlockFace face : CARDINAL_FACES) {
            Block relative = placedBlock.getRelative(face);
            if (relative.getType() == Material.CHEST) {
                PlayerShop shop = shopManager.getShopAtLocation(relative.getLocation());
                if (shop != null) {
                    event.setCancelled(true);
                    MessageUtil.msg(event.getPlayer(), Lang.get("shop.double-chest-blocked"));
                    return;
                }
            }
        }
    }

    // ===================== INVENTARIO =====================
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Chest chest)) return;

        PlayerShop shop = shopManager.getShopAtLocation(chest.getLocation());
        if (shop != null) shop.updateSign();
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Chest chest)) return;

        PlayerShop shop = shopManager.getShopAtLocation(chest.getLocation());
        if (shop != null) shop.updateSign();
    }

    // ===================== ROMPER TIENDA =====================
    @EventHandler
    public void onShopBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerShop shop = getShopFromBlock(event.getBlock());
        if (shop == null) return;

        if (!shop.getOwner().equals(player.getUniqueId()) && !player.hasPermission("qsmarket.admin")) {
            event.setCancelled(true);
            MessageUtil.msg(player, Lang.get("shop.cannot-break-others"));
            return;
        }

        shop.destroyShop(false);
        shopManager.removeShop(shop);

        if (!shop.getOwner().equals(player.getUniqueId())) {
            MessageUtil.msg(player, Lang.get("admin.broken-by-admin"),
                    Placeholder.parsed("owner", shop.getOwnerName()));
        } else {
            MessageUtil.msg(player, Lang.get("shop.deleted"));
        }
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
        PlayerShop shop = shopManager.getShopAtLocation(block.getLocation());
        if (shop != null) return shop;

        if (block.getState() instanceof Chest chest) {
            org.bukkit.block.DoubleChest doubleChest =
                    chest.getInventory().getHolder() instanceof org.bukkit.block.DoubleChest
                            ? (org.bukkit.block.DoubleChest) chest.getInventory().getHolder() : null;

            if (doubleChest != null) {
                Chest leftSide = (Chest) doubleChest.getLeftSide();
                Chest rightSide = (Chest) doubleChest.getRightSide();

                if (leftSide != null) {
                    shop = shopManager.getShopAtLocation(leftSide.getLocation());
                    if (shop != null) return shop;
                }
                if (rightSide != null) {
                    shop = shopManager.getShopAtLocation(rightSide.getLocation());
                    if (shop != null) return shop;
                }
            }
        }

        if (block.getState() instanceof Sign) {
            for (PlayerShop s : shopManager.getAllShops()) {
                if (s.getSignLocation().equals(block.getLocation()))
                    return s;
            }
        }
        return null;
    }
}