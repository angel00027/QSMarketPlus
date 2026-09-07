package mp.quesito.qSMarketPlus.listeners;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.hooks.impl.QsProteccionHook;
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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class PlayerShopListener implements Listener {

    private final PlayerShopManager shopManager =
            QSMarketPlus.getInstance().getShopManager();

    private final java.util.logging.Logger logger =
            QSMarketPlus.getInstance().getLogger();

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

        // ===================== VINCULACIÓN CON REDSTONE / ITEM REQUERIDO =====================
        ItemStack handItem = player.getInventory().getItemInMainHand();
        boolean linkItemInHand = isLinkItem(handItem);

        // Si require-item está activo pero el jugador usa el viejo trigger (redstone) → aviso
        if (!linkItemInHand && requireItemEnabled()
                && handItem != null && handItem.getType() == Material.REDSTONE
                && (block.getState() instanceof Chest || block.getState() instanceof Sign)) {
            MessageUtil.msg(player, Lang.get("shop.required-item-missing"),
                    Placeholder.parsed("item", requiredItemName()));
            event.setCancelled(true);
            return;
        }

        if (linkItemInHand) {

            // ===== SELECCIONAR COFRE =====
            if (block.getState() instanceof Chest) {
                Location loc = block.getLocation();

                // ===== 🛡️ VALIDAR COFRE (tienda / protección / región) =====
                String error = validarCofreParaTienda(player, loc);
                if (error != null) {
                    MessageUtil.msg(player, Lang.get(error));
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

                // ===== 🛡️ RE-VALIDAR COFRE EN LA VINCULACIÓN =====
                // Se vuelve a comprobar aquí para evitar el hueco que deja
                // validar solo al seleccionar (el cofre pudo protegerse después).
                String errorVinculacion = validarCofreParaTienda(player, chestLoc);
                if (errorVinculacion != null) {
                    MessageUtil.msg(player, Lang.get(errorVinculacion));
                    cleanup(player);
                    event.setCancelled(true);
                    return;
                }

                PlayerShop existingShop = shopManager.getShopAtLocation(signLoc);
                if (existingShop != null && !existingShop.getOwner().equals(player.getUniqueId())) {
                    String ownerName = Bukkit.getOfflinePlayer(existingShop.getOwner()).getName();
                    MessageUtil.msg(player, Lang.get("shop.cannot-link-sign"),
                            Placeholder.parsed("owner", ownerName != null ? ownerName : "Desconocido"));
                    cleanup(player);
                    return;
                }

                // ===== 🛡️ EL CARTEL PERTENECE A UNA PROTECCIÓN =====
                if (QsProteccionHook.isAvailable() && QsProteccionHook.isProtectionSign(block)) {
                    MessageUtil.msg(player, Lang.get("shop.sign-protected"));
                    cleanup(player);
                    event.setCancelled(true);
                    return;
                }

                // ===== 📏 LÍMITE DE DISTANCIA CARTEL-COFRE =====
                int maxDistance = QSMarketPlus.getInstance().getConfig()
                        .getInt("player-shops.max-sign-distance", 0);
                if (maxDistance > 0) {
                    double distance = chestBlock.getLocation().distance(block.getLocation());
                    if (distance > maxDistance) {
                        MessageUtil.msg(player, Lang.get("shop.sign-too-far"),
                                Placeholder.parsed("distance", String.valueOf(maxDistance)));
                        cleanup(player);
                        event.setCancelled(true);
                        return;
                    }
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

                // 📥 Consumir item requerido (si está configurado)
                consumeLinkItem(player);

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

        // ===== 🛡️ COFRE PROTEGIDO POR OTRO JUGADOR =====
        if (QsProteccionHook.isAvailable()) {
            Location protectedLoc =
                    QsProteccionHook.getProtectedChestLocation(shop.getChestLocation());
            if (protectedLoc != null
                    && !QsProteccionHook.isOwner(protectedLoc, shop.getOwner())) {

                boolean privileged = player.hasPermission("qsmarket.admin")
                        || QsProteccionHook.isOwner(protectedLoc, player.getUniqueId());

                if (!privileged) {
                    event.setCancelled(true);
                    MessageUtil.msg(player, Lang.get("shop.shop-protected-by-other"));
                    return;
                }
            }
        }

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

    // ===================== ITEM REQUERIDO =====================
    private boolean requireItemEnabled() {
        return QSMarketPlus.getInstance().getConfig()
                .getBoolean("player-shops.require-item.enabled", false);
    }

    private Material getLinkMaterial() {
        if (requireItemEnabled()) {
            String matName = QSMarketPlus.getInstance().getConfig()
                    .getString("player-shops.require-item.material", "EMERALD");
            Material mat = Material.matchMaterial(matName != null ? matName : "EMERALD");
            if (mat != null) return mat;
        }
        return Material.REDSTONE;
    }

    private boolean isLinkItem(ItemStack item) {
        if (item == null) return false;
        if (item.getType() != getLinkMaterial()) return false;

        if (requireItemEnabled()) {
            ConfigurationSection req = QSMarketPlus.getInstance().getConfig()
                    .getConfigurationSection("player-shops.require-item");

            int cmd = req.getInt("custom-model-data", 0);
            if (cmd > 0) {
                ItemMeta meta = item.getItemMeta();
                if (meta == null || !meta.hasCustomModelData()
                        || meta.getCustomModelData() != cmd) {
                    return false;
                }
            }

            String displayName = req.getString("display-name", "");
            if (displayName != null && !displayName.isEmpty()) {
                ItemMeta meta = item.getItemMeta();
                String itemName = meta != null && meta.hasDisplayName()
                        ? meta.getDisplayName() : "";
                if (!ChatColor.stripColor(itemName)
                        .equalsIgnoreCase(ChatColor.stripColor(displayName))) {
                    return false;
                }
            }
        }
        return true;
    }

    private String requiredItemName() {
        if (requireItemEnabled()) {
            String name = QSMarketPlus.getInstance().getConfig()
                    .getString("player-shops.require-item.display-name", "");
            if (name != null && !name.isEmpty()) {
                return ChatColor.stripColor(name);
            }

            String matName = QSMarketPlus.getInstance().getConfig()
                    .getString("player-shops.require-item.material", "EMERALD");
            Material mat = Material.matchMaterial(matName != null ? matName : "EMERALD");
            if (mat != null) {
                String n = mat.name().replace("_", " ").toLowerCase();
                return Character.toUpperCase(n.charAt(0)) + n.substring(1);
            }
        }
        return "Redstone";
    }

    private void consumeLinkItem(Player player) {
        ConfigurationSection req = QSMarketPlus.getInstance().getConfig()
                .getConfigurationSection("player-shops.require-item");
        if (req == null || !req.getBoolean("enabled", false)) return;
        if (!req.getBoolean("consume", true)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != getLinkMaterial()) return;

        item.setAmount(item.getAmount() - 1);
        player.updateInventory();
    }

    // ===================== VALIDACIÓN DE COFRE =====================

    /**
     * Valida si un cofre puede usarse para crear una tienda.
     *
     * Se ejecuta tanto al SELECCIONAR el cofre como al
     * VINCULAR el cartel, para evitar huecos entre ambos clicks.
     *
     * @return null si es válido, o la clave de mensaje de error.
     */
    private String validarCofreParaTienda(Player player, Location loc) {

        // ===== 1) ¿Ya es una tienda (o mitad de cofre tienda)? =====
        if (esCofreTienda(loc)) {
            logger.info("[PlayerShop] " + player.getName()
                    + " intentó usar un cofre que ya es tienda: " + loc);
            return "shop.already-exists";
        }

        // ===== 2) ¿Protegido por QsProteccion? =====
        if (QsProteccionHook.isAvailable()) {

            Location protectedLoc =
                    QsProteccionHook.getProtectedChestLocation(loc);

            if (protectedLoc != null) {

                UUID owner = QsProteccionHook.getOwner(protectedLoc);

                boolean canBypass = player.hasPermission("qsmarket.admin")
                        || player.hasPermission("qsmarket.shop.bypass.protection")
                        || (QSMarketPlus.getInstance().getConfig()
                        .getBoolean("player-shops.allow-own-protected-chest", true)
                        && owner != null && owner.equals(player.getUniqueId()));


                if (!canBypass) {
                    return "shop.chest-protected";
                }
            }

        } else {

            logger.info("[PlayerShop] QsProteccion NO disponible: "
                    + "no se aplica chequeo de protección para " + loc);
        }

        // ===== 3) ¿Permiso en la región de ProtectionStones? =====
        if (QSMarketPlus.getInstance().getConfig()
                .getBoolean("player-shops.require-region-permission", true)
                && QsProteccionHook.isAvailable()) {

            boolean regionOk = player.hasPermission("qsmarket.admin")
                    || player.hasPermission("qsmarket.shop.bypass.protection")
                    || QsProteccionHook.canCreateInRegion(loc, player.getUniqueId());


            if (!regionOk) {
                return "shop.no-region-permission";
            }
        }

        return null;
    }

    /**
     * Comprueba si la ubicación es el cofre de una tienda
     * (o una mitad de un cofre doble que es tienda).
     */
    private boolean esCofreTienda(Location loc) {

        if (shopManager.getShopAtLocation(loc) != null) {
            return true;
        }

        Block block = loc.getBlock();

        if (block.getState() instanceof Chest chest) {

            if (chest.getInventory().getHolder()
                    instanceof org.bukkit.block.DoubleChest doubleChest) {

                Chest left = (Chest) doubleChest.getLeftSide();
                Chest right = (Chest) doubleChest.getRightSide();

                if (left != null
                        && shopManager.getShopAtLocation(left.getLocation()) != null) {
                    return true;
                }

                if (right != null
                        && shopManager.getShopAtLocation(right.getLocation()) != null) {
                    return true;
                }
            }
        }

        return false;
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

        if (shop != null) {
            return shop;
        }

        if (block.getState() instanceof Chest chest) {
            org.bukkit.block.DoubleChest doubleChest =
                    chest.getInventory().getHolder() instanceof org.bukkit.block.DoubleChest
                            ? (org.bukkit.block.DoubleChest) chest.getInventory().getHolder() : null;

            if (doubleChest != null) {
                Chest leftSide = (Chest) doubleChest.getLeftSide();
                Chest rightSide = (Chest) doubleChest.getRightSide();

                if (leftSide != null) {
                    shop = shopManager.getShopAtLocation(leftSide.getLocation());
                    if (shop != null) {

                        return shop;
                    }
                }
                if (rightSide != null) {
                    shop = shopManager.getShopAtLocation(rightSide.getLocation());
                    if (shop != null) {
                        return shop;
                    }
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