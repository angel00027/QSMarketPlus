package mp.quesito.qSMarketPlus.shop;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.holder.ConfirmHolder;
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

public class ConfirmMenu {

    public static void open(Player player, ShopItem item, ShopCategory category, int amount, boolean buying) {

        FileConfiguration cfg = QSMarketPlus.getInstance().confirmMenuConfig.getConfig();

        // =============================
        //          TÍTULO
        // =============================
        String baseTitle = buying
                ? cfg.getString("menu.title-buy", "<green>Confirmar compra")
                : cfg.getString("menu.title-sell", "<red>Confirmar venta");

        String title = MessageUtil.toLegacy(baseTitle);

        // =============================
        //       CREAR INVENTARIO
        // =============================
        Inventory inv = Bukkit.createInventory(
                new ConfirmHolder(item, category, amount, buying),
                cfg.getInt("menu.rows", 3) * 9,
                title
        );

        // =============================
        //       DECORACIÓN OPCIONAL
        // =============================
        if (cfg.getBoolean("menu.fill-background", true)) {
            ItemStack glass = MenuItems.glass();
            for (int i = 0; i < inv.getSize(); i++) {
                inv.setItem(i, glass);
            }
        }

        // =============================
        //       ITEM CENTRAL
        // =============================
        int infoSlot = cfg.getInt("menu.info-slot", 13);
        inv.setItem(infoSlot, MenuItems.confirmInfoItem(item, amount, buying));

        // =============================
        //       BOTONES CONFIGURADOS
        // =============================
        loadButton(cfg, inv, "buttons.confirm", MenuItems.confirmButton());
        loadButton(cfg, inv, "buttons.cancel",  MenuItems.cancelButton());
        loadButton(cfg, inv, "buttons.back",    MenuItems.backButton());

        player.openInventory(inv);
    }


    // =============================================================
    //             FUNCIÓN MEJORADA PARA CARGAR BOTONES
    // =============================================================
    private static void loadButton(FileConfiguration cfg, Inventory inv, String path, ItemStack fallback) {

        if (!cfg.isConfigurationSection(path)) return;

        ConfigurationSection sec = cfg.getConfigurationSection(path);
        if (!sec.getBoolean("enabled", true)) return;

        // Construir botón desde config
        ItemStack button = MenuItems.buttonFromConfig(sec);
        if (button == null) button = fallback;

        // === AÑADIR TAG SEGÚN EL NOMBRE DEL PATH ===
        String tag = path.substring(path.lastIndexOf(".") + 1);  // confirm, cancel, back

        ItemMeta meta = button.getItemMeta();
        MetaUtil.setTag(meta, "btn", tag.toLowerCase());
        button.setItemMeta(meta);

        // Colocar botón
        int slot = sec.getInt("slot", -1);
        if (slot >= 0 && slot < inv.getSize()) {
            inv.setItem(slot, button);
        }
    }

}
