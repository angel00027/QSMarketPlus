package mp.quesito.qSMarketPlus.shop;

import mp.quesito.qSMarketPlus.utils.ItemSerializer;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ShopItem {

    private final String id;
    private final String name;
    private final double buy;
    private final double sell;

    // Base64 del ItemStack completo
    private String itemstackBase64;

    private boolean onlyOnce;
    private String permission;
    private String requiredPermission;
    private String requiredGroup;
    private List<String> lore;
    private List<String> commands;
    private List<String> sellCommands;

    public ShopItem(String id, String name, double buy, double sell, String itemstackBase64) {
        this.id = id;
        this.name = name;
        this.buy = buy;
        this.sell = sell;
        this.itemstackBase64 = itemstackBase64;
    }

    // ================= GETTERS =================
    public String getId() { return id; }
    public String getName() { return name; }
    public double getBuy() { return buy; }
    public double getSell() { return sell; }

    /**
     * Devuelve el ItemStack real deserializado desde Base64.
     * Si Base64 es null o falla, devuelve un ItemStack de NETHERITE como fallback.
     */
    public ItemStack getRealItem() {
        if (itemstackBase64 != null && !itemstackBase64.isEmpty()) {
            ItemStack deserialized = ItemSerializer.fromBase64(itemstackBase64);
            if (deserialized != null) return deserialized;
        }
        return new ItemStack(Material.NETHERITE_BLOCK); // fallback visual si algo falla
    }

    public Material getMaterial() {
        ItemStack item = getRealItem();
        return item != null ? item.getType() : Material.AIR;
    }

    public List<String> getLore() { return lore != null ? lore : List.of(); }
    public List<String> getCommands() { return commands != null ? commands : List.of(); }
    public List<String> getSellCommands() { return sellCommands != null ? sellCommands : List.of(); }

    // ================= SETTERS =================
    public void setLore(List<String> lore) { this.lore = lore; }
    public void setOnlyOnce(boolean onlyOnce) { this.onlyOnce = onlyOnce; }
    public void setPermission(String permission) { this.permission = permission; }
    public void setRequiredPermission(String requiredPermission) { this.requiredPermission = requiredPermission; }
    public void setRequiredGroup(String requiredGroup) { this.requiredGroup = requiredGroup; }
    public void setCommands(List<String> commands) { this.commands = commands; }
    public void setSellCommands(List<String> sellCommands) { this.sellCommands = sellCommands; }

    public void setItemstackBase64(String base64) { this.itemstackBase64 = base64; }

    // ================= EJECUTAR COMANDOS =================
    public boolean executeBuyCommands(Player player) {
        if (commands == null || commands.isEmpty()) return false;

        for (String cmd : commands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
        }

        player.sendMessage("§a¡Has adquirido §f" + MessageUtil.stripLegacy(name) + "§a!");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);

        return true;
    }

    public boolean executeSellCommands(Player player) {
        if (sellCommands == null || sellCommands.isEmpty()) return false;

        for (String cmd : sellCommands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
        }

        return true;
    }

    // ================= FLAGS =================
    public boolean isOnlyOnce() { return onlyOnce; }
    public String getPermission() { return permission; }
    public String getRequiredPermission() { return requiredPermission; }
    public String getRequiredGroup() { return requiredGroup; }

    // ================= ACCESO =================
    public boolean canAccess(Player player) {
        if (requiredPermission != null && !requiredPermission.isEmpty() && !player.hasPermission(requiredPermission)) return false;
        if (requiredGroup != null && !requiredGroup.isEmpty() && !player.hasPermission("group." + requiredGroup)) return false;
        return true;
    }
}
