package mp.quesito.qSMarketPlus.shop;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.holder.AmountHolder;
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

public class AmountMenu {

    public static void open(Player player, AmountHolder holder) {

        FileConfiguration cfg = QSMarketPlus.getInstance().amountMenuConfig.getConfig();

        boolean buying = holder.isBuying();
        int amount = holder.getAmount();
        int maxStack = holder.getItem().getMaterial().getMaxStackSize();

        // ===================================================
        //                    TÍTULO
        // ===================================================
        String title = buying
                ? cfg.getString("menu.title-buy", "<green>Selecciona cantidad")
                : cfg.getString("menu.title-sell", "<red>Selecciona cantidad");

        title = MessageUtil.toLegacy(title);

        // ===================================================
        //          TAMAÑO DEL INVENTARIO DESDE CONFIG
        // ===================================================
        int size = cfg.getInt("menu.size", 27); // <--- AHORA CONFIGURABLE
        Inventory inv = Bukkit.createInventory(holder, size, title);

        // ===================================================
        //                     INFO SLOT
        // ===================================================
        int infoSlot = cfg.getInt("buttons.info-slot", 13);
        inv.setItem(infoSlot, MenuItems.infoAmountItem(holder.getItem(), amount, buying));

        // ===================================================
        //         CARGAR TODOS LOS BOTONES DINÁMICAMENTE
        // ===================================================
        if (cfg.isConfigurationSection("buttons")) {

            ConfigurationSection buttons = cfg.getConfigurationSection("buttons");

            for (String key : buttons.getKeys(false)) {
                loadConfiguredButton(cfg, inv, "buttons." + key);
            }
        }

        player.openInventory(inv);
    }

    // ====================================================================
    //       CARGA UN BOTÓN DESDE CONFIG Y AÑADE SUS TAGS PERSONALIZADOS
    // ====================================================================
    private static void loadConfiguredButton(FileConfiguration cfg, Inventory inv, String path) {

        if (!cfg.isConfigurationSection(path)) return;
        ConfigurationSection sec = cfg.getConfigurationSection(path);

        if (!sec.getBoolean("enabled", true)) return;

        // Crear botón desde config
        ItemStack button = MenuItems.buttonFromConfig(sec);
        if (button == null) return;

        ItemMeta meta = button.getItemMeta();

        // TAG PRINCIPAL (btn)
        if (sec.contains("tag")) {
            MetaUtil.setTag(meta, "btn", sec.getString("tag"));
        }

        // VALUE (numérico o string)
        if (sec.contains("value")) {
            String v = sec.getString("value");
            MetaUtil.setTag(meta, "value", v);

            // Si es MIN/MAX, marcarlo como especial también
            if ("MIN".equalsIgnoreCase(v) || "MAX".equalsIgnoreCase(v)) {
                MetaUtil.setTag(meta, "special", v.toUpperCase());
            }
        }
        // AMOUNT extra (solo visual, no afecta la lógica)

        if (sec.contains("amount")) {

            int visualAmount = sec.getInt("amount", 1);

            // Máximo 64 porque Minecraft no permite más en GUI
            visualAmount = Math.max(1, Math.min(64, visualAmount));

            button.setAmount(visualAmount);

            // Guardamos amount en el PDC si quieres usarlo luego
            MetaUtil.setTag(meta, "amount", String.valueOf(visualAmount));
        }

        button.setItemMeta(meta);

        // Colocar en inventario
        int slot = sec.getInt("slot", -1);
        if (slot >= 0) inv.setItem(slot, button);
    }
}
