package mp.quesito.qSMarketPlus.shop;

import mp.quesito.qSMarketPlus.holder.CategoryHolder;
import mp.quesito.qSMarketPlus.manager.CategoryManager;
import mp.quesito.qSMarketPlus.utils.MenuItems;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import mp.quesito.qSMarketPlus.utils.MetaUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CategoryMenu {

    public static void open(Player player, CategoryManager manager) {

        FileConfiguration cfg = manager.getConfig();

        int rows = cfg.getInt("menu.rows", 6);
        int size = rows * 9;

        Inventory inv = Bukkit.createInventory(
                new CategoryHolder(),
                size,
                MessageUtil.toLegacy(cfg.getString("menu.title", "<green>QSMarketPlus</green>"))
        );

        // ====================================
        //        DECORACIÓN BORDES
        // ====================================
        ItemStack decor = MenuItems.glass();
        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9) {
                inv.setItem(i, decor);
            }
        }

        // ====================================
        //        COLOCAR CATEGORÍAS
        // ====================================
        for (ShopCategory cat : manager.getCategories().values()) {

            boolean locked = !cat.canAccess(player);
            List<String> loreFinal = new ArrayList<>(cat.getLore());

            if (locked) {
                loreFinal.add("");
                if (cat.getRequiredPermission() != null) {
                    loreFinal.add("§cRequiere: §7" + cat.getRequiredPermission());
                }
                if (cat.getRequiredGroup() != null) {
                    loreFinal.add("§cGrupo requerido: §7" + cat.getRequiredGroup());
                }
                loreFinal.add("");
                loreFinal.add("§c❌ No tienes acceso");
            }

            // ================================
            //    ARMAR ITEM DE LA CATEGORÍA
            // ================================
            ItemStack item;

            if (cat.getHeadTexture() != null && !cat.getHeadTexture().isEmpty()) {
                item = MenuItems.customHead(cat.getName(), cat.getHeadTexture());
            } else {
                item = new ItemStack(cat.getMaterial());
            }

            ItemMeta meta = item.getItemMeta();

            String displayName = MessageUtil.toLegacy(cat.getName());
            if (locked) {
                meta.setDisplayName("§c" + MessageUtil.stripLegacy(displayName));
            } else {
                meta.setDisplayName(displayName);
            }

            // Lore coloreada
            List<String> coloredLore = new ArrayList<>();
            for (String line : loreFinal) {
                coloredLore.add(MessageUtil.toLegacy(line));
            }
            meta.setLore(coloredLore);

            // Tags
            MetaUtil.setTag(meta, "category", cat.getId());
            if (locked) {
                MetaUtil.setTag(meta, "locked", "true");
            }

            item.setItemMeta(meta);
            inv.setItem(cat.getSlot(), item);
        }

        // ====================================
        //             BOTÓN CERRAR
        // ====================================
        ConfigurationSection closeSec = cfg.getConfigurationSection("buttons.close");
        if (closeSec != null) {
            ItemStack closeBtn = MenuItems.buttonFromConfig(closeSec);
            ItemMeta meta = closeBtn.getItemMeta();
            MetaUtil.setTag(meta, "btn", "close");
            closeBtn.setItemMeta(meta);
            inv.setItem(closeSec.getInt("slot", size - 5), closeBtn);
        }

        // ====================================
        //          BOTÓN VENDER TODO
        // ====================================
        ConfigurationSection sellSec = cfg.getConfigurationSection("buttons.sell-all");
        if (sellSec != null) {
            ItemStack sellBtn = MenuItems.buttonFromConfig(sellSec);
            ItemMeta meta = sellBtn.getItemMeta();
            MetaUtil.setTag(meta, "btn", "sell_all");
            sellBtn.setItemMeta(meta);
            inv.setItem(sellSec.getInt("slot", size - 2), sellBtn);
        }

        player.openInventory(inv);
    }
}
