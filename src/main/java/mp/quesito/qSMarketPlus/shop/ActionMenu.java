package mp.quesito.qSMarketPlus.shop;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.holder.ActionHolder;
import mp.quesito.qSMarketPlus.utils.MenuItems;
import mp.quesito.qSMarketPlus.utils.NexoGui;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import mp.quesito.qSMarketPlus.utils.MetaUtil;
import org.bukkit.Bukkit;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ActionMenu {

    public static void open(Player player, ShopItem item, ShopCategory category) {

        // ✔️ ESTA ES LA LÍNEA CORRECTA
        FileConfiguration cfg = QSMarketPlus.getInstance().actionMenuConfig.getConfig();

        // ==============================
        //        TÍTULO / FILAS
        // ==============================
        String title = MessageUtil.toLegacy(NexoGui.title(
                cfg.getString("menu.title", "<yellow>Selecciona una acción")
        ));

        int rows = cfg.getInt("menu.rows", 3);
        int size = rows * 9;

        Inventory inv = Bukkit.createInventory(
                new ActionHolder(item, category),
                size,
                title
        );

        // ==============================
        //       FONDO / DECORACIÓN
        // ==============================
        if (cfg.getBoolean("menu.fill-background", true)) {
            ItemStack glass = MenuItems.glass();
            for (int i = 0; i < size; i++) {
                inv.setItem(i, glass);
            }
        }

        // ==============================
        //     BOTONES CONFIGURADOS
        // ==============================
        loadButton(cfg, inv, "buttons.buy",  MenuItems.buyButton(player,item),  item);
        loadButton(cfg, inv, "buttons.sell", MenuItems.sellButton(item), item);
        loadButton(cfg, inv, "buttons.back", MenuItems.backButton(),    item);

        int itemSlot = cfg.getInt("menu.item-slot", 13);
        if (itemSlot >= 0 && itemSlot < size) {
            ItemStack shown = item.getRealItem().clone();
            shown.setAmount(1);
            ItemMeta shownMeta = shown.getItemMeta();
            if (shownMeta != null) {
                shownMeta.setDisplayName(MessageUtil.toLegacy(item.getName()));
                List<String> lore = new ArrayList<>();
                String buy = MessageUtil.priceFormat("buy", item);
                String sell = MessageUtil.priceFormat("sell", item);
                if (!buy.isEmpty()) lore.add(buy);
                if (!sell.isEmpty()) lore.add(sell);
                shownMeta.setLore(lore);
                shown.setItemMeta(shownMeta);
            }
            inv.setItem(itemSlot, shown);
        }

        player.openInventory(inv);
    }

    // ==========================================
    //      FUNCIÓN PARA CARGAR BOTONES YML
    // ==========================================
    private static void loadButton(FileConfiguration cfg, Inventory inv, String path, ItemStack fallback, ShopItem item) {

        if (!cfg.isConfigurationSection(path)) return;

        ConfigurationSection sec = cfg.getConfigurationSection(path);
        if (!sec.getBoolean("enabled", true)) return;

        // Crear botón desde YML
        ItemStack button = MenuItems.buttonFromConfig(sec);
        if (button == null) button = fallback;

        // ---------- TAG AUTOMÁTICO ----------
        // path "buttons.buy" → tag = "buy"
        String tag = path.substring(path.lastIndexOf(".") + 1);

        ItemMeta meta = button.getItemMeta();
        MetaUtil.setTag(meta, "btn", tag);
        button.setItemMeta(meta);

        // ---------- SLOT ----------
        int slot = sec.getInt("slot", -1);
        if (slot >= 0 && slot < inv.getSize()) {
            inv.setItem(slot, button);
        }
    }
}
