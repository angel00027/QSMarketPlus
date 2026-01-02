package mp.quesito.qSMarketPlus.shop;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.utils.ItemSerializer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class PlayerShop {

    private final UUID owner;
    private final Location chestLocation;
    private final Location signLocation;
    private final String ownerName;

    private final ShopItemHologram hologram;

    private double price;           // Precio por venta (desde DB)
    private int amountPerSale;      // Cantidad por venta (desde DB)
    private boolean active = true;

    public PlayerShop(UUID owner, Location chestLocation, Location signLocation, int amountPerSale, double price) {
        this.owner = owner;
        this.chestLocation = chestLocation;
        this.signLocation = signLocation;
        this.amountPerSale = amountPerSale;
        this.price = price;
        this.ownerName = Bukkit.getOfflinePlayer(owner).getName();
        this.hologram = new ShopItemHologram(this);
    }

    // ===================== GETTERS / SETTERS =====================
    public UUID getOwner() { return owner; }
    public Location getChestLocation() { return chestLocation; }
    public Location getSignLocation() { return signLocation; }
    public String getOwnerName() { return ownerName; }

    public double getPrice() { return price; }
    public int getAmountPerSale() { return amountPerSale; }

    public void setPrice(double price) { this.price = price; }
    public void setAmountPerSale(int amount) { this.amountPerSale = amount; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    private static final double HOLOGRAM_RADIUS = QSMarketPlus.getInstance().getConfig().getDouble("holograms.radius", 16);


    public ShopItemHologram getHologram() { return hologram; }

    // ===================== INVENTARIO DEL COFRE =====================
    public Inventory getChestInventory() {
        if (chestLocation.getBlock().getState() instanceof Chest chest) return chest.getInventory();
        return null;
    }

    private int getAvailableStock(Inventory inv, Material type) {
        return Arrays.stream(inv.getContents())
                .filter(i -> i != null && i.getType() == type)
                .mapToInt(ItemStack::getAmount)
                .sum();
    }

    // ===================== COMPRA =====================
    public void buy(Player buyer) {

        if (!active) {
            buyer.sendMessage("§cEsta tienda está inactiva.");
            return;
        }

        Inventory inv = getChestInventory();
        if (inv == null) {
            deactivateBrokenShop();
            return;
        }

        ItemStack firstItem = Arrays.stream(inv.getContents())
                .filter(i -> i != null && i.getType() != Material.AIR)
                .findFirst()
                .orElse(null);

        if (firstItem == null) {
            buyer.sendMessage("§cNo hay items disponibles.");
            updateSign();
            return;
        }

        Material mainType = firstItem.getType();
        ItemStack soldItem = firstItem.clone();

        // Verificar items mezclados
        boolean mixed = Arrays.stream(inv.getContents())
                .filter(i -> i != null && i.getType() != Material.AIR)
                .anyMatch(i -> i.getType() != mainType);

        if (mixed) {
            buyer.sendMessage("§cEsta tienda tiene items mezclados.");
            Player ownerOnline = Bukkit.getPlayer(owner);
            if (ownerOnline != null)
                ownerOnline.sendMessage("§cTu tienda tiene items mezclados. Solo se permite vender: §e" + mainType);
            return;
        }

        int totalStock = getAvailableStock(inv, mainType);

        // Stock insuficiente
        if (totalStock < amountPerSale) {
            buyer.sendMessage("§cNo hay suficientes items disponibles para esta venta. Stock: §e" + totalStock);
            updateSign();
            return; // No se desactiva la tienda
        }

        // Dinero insuficiente
        if (!QSMarketPlus.economy.has(buyer, price)) {
            buyer.sendMessage("§cNo tienes suficiente dinero. Necesitas: §6$" + price);
            return;
        }

        // Backup del inventario
        ItemStack[] backup = inv.getContents().clone();

        // Quitar items
        int remaining = amountPerSale;
        for (ItemStack stack : inv.getContents()) {
            if (stack == null || stack.getType() != mainType) continue;
            int take = Math.min(stack.getAmount(), remaining);
            stack.setAmount(stack.getAmount() - take);
            remaining -= take;
            if (remaining <= 0) break;
        }

        if (remaining > 0) { // Error interno
            inv.setContents(backup);
            buyer.sendMessage("§cError interno al procesar la compra.");
            return;
        }

        // Dar items
        ItemStack give = soldItem.clone();
        give.setAmount(amountPerSale);

        if (!buyer.getInventory().addItem(give).isEmpty()) {
            inv.setContents(backup);
            buyer.sendMessage("§cNo tienes espacio en tu inventario.");
            return;
        }

        // Cobrar y depositar
        QSMarketPlus.economy.withdrawPlayer(buyer, price);
        OfflinePlayer ownerOffline = Bukkit.getOfflinePlayer(owner);
        QSMarketPlus.economy.depositPlayer(ownerOffline, price);

        // Mensajes
        buyer.sendMessage("§aHas comprado §ex" + amountPerSale + " " + getItemName(soldItem) + " §apor §6$" + price);
        if (ownerOffline.isOnline()) {
            ((Player) ownerOffline).sendMessage("§aTu tienda vendió §ex" + amountPerSale + " " + getItemName(soldItem) + " §apor §6$" + price);
        }

        updateSign(); // Actualiza cartel y holograma
    }

    // ===================== ACTUALIZACION DE CARTEL =====================
    public void updateSign() {
        if (!(signLocation.getBlock().getState() instanceof Sign sign)) return;

        Inventory inv = getChestInventory();
        ItemStack firstItem = null;
        int stock = 0;
        boolean mixed = false;

        if (inv != null) {
            for (ItemStack item : inv.getContents()) {
                if (item == null || item.getType() == Material.AIR) continue;
                if (firstItem == null) firstItem = item;
                else if (item.getType() != firstItem.getType()) mixed = true;

                if (!mixed && item.getType() == firstItem.getType()) stock += item.getAmount();
            }
        }

        String line2, line3;
        if (firstItem == null) {
            line2 = "§e$" + price + " §dx" + amountPerSale;
            line3 = "§cSin stock : 0";
        } else if (mixed) {
            line2 = "§cItems mezclados";
            line3 = "§eRevisa el cofre";
        } else {
            ChatColor stockColor = stock >= amountPerSale ? ChatColor.WHITE : ChatColor.RED;
            line2 = "§e$" + price + " §dx" + amountPerSale;
            line3 = "§6" + getItemName(firstItem) + " §7: " + stockColor + stock;
        }

        sign.setLine(0, "§aTienda de");
        sign.setLine(1, "§b" + ownerName);
        sign.setLine(2, line2);
        sign.setLine(3, line3);
        sign.update();

        // Holograma
        if (hologram != null) {
            updateHologramVisibility(); // Decide si mostrar o quitar
        }
    }


    public void updateHologramVisibility() {
        if (!QSMarketPlus.getInstance().getConfig().getBoolean("holograms.enabled")) {
            hologram.remove();
            return;
        }

        Location loc = chestLocation;
        boolean playerNearby = loc.getWorld().getPlayers().stream()
                .anyMatch(p -> p.getLocation().distanceSquared(loc) <= HOLOGRAM_RADIUS * HOLOGRAM_RADIUS);

        Inventory inv = getChestInventory();
        ItemStack firstItem = null;
        int stock = 0;

        if (inv != null) {
            for (ItemStack item : inv.getContents()) {
                if (item == null || item.getType() == Material.AIR) continue;
                if (firstItem == null) firstItem = item;
                else if (item.getType() != firstItem.getType()) break;
                stock += item.getAmount();
            }
        }

        if (playerNearby && firstItem != null && stock >= amountPerSale) {
            hologram.spawn();
        } else {
            hologram.remove();
        }
    }

    // ===================== UTILIDADES =====================
    public String getItemName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) return meta.getDisplayName();
        String name = item.getType().name().replace("_", " ").toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }



    // ===================== SERIALIZACION =====================
    public String serializeItems() {
        Inventory inv = getChestInventory();
        if (inv == null) return "";
        return ItemSerializer.toBase64Array(inv.getContents());
    }

    public void deserializeItems(String base64) {
        Inventory inv = getChestInventory();
        if (inv == null) return;

        inv.clear();
        ItemStack[] items = ItemSerializer.fromBase64Array(base64);
        if (items != null) {
            for (ItemStack item : items) {
                if (item != null && item.getType() != Material.AIR) inv.addItem(item);
            }
        }
        updateSign();
    }

    private void deactivateBrokenShop() {
        this.active = false;
        QSMarketPlus.getInstance().getLogger().warning("La tienda de " + owner + " se desactivó automáticamente (cofre roto).");
    }

    public void destroyShop(boolean removeChest) {
        // Quitar holograma
        if (hologram != null) {
            hologram.remove();
        }

        // Eliminar solo el cartel
        Block signBlock = signLocation.getBlock();
        signBlock.setType(Material.AIR);

        // Eliminar cofre solo si se indica
        if (removeChest) {
            Block chestBlock = chestLocation.getBlock();
            chestBlock.setType(Material.AIR);
        }

        active = false;
    }

}
