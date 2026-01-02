package mp.quesito.qSMarketPlus.auction.menu;

import mp.quesito.qSMarketPlus.auction.holder.AHConfirmHolder;
import mp.quesito.qSMarketPlus.utils.MetaUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

public class AHConfirmMenu {

    private static final String TITLE = "Confirmar Subasta";

    // ============================================================
    // 📌 CONFIRMAR ÍTEM ÚNICO – usando slot original
    // ============================================================
    public static void openSingle(Player p, ItemStack original, double price, int slot) {

        // Backup exacto del ítem original (para devolverlo si cancela)
        ItemStack backup = original.clone();

        // Creamos el holder con: item, precio, backup, slotOriginal
        AHConfirmHolder holder = new AHConfirmHolder(original.clone(), price, backup, slot);

        Inventory inv = Bukkit.createInventory(holder, 27, TITLE);

        fillBackground(inv);

        inv.setItem(11, previewItem(original.clone()));
        inv.setItem(13, pricePaper(price));
        inv.setItem(15, confirmButton());
        inv.setItem(26, cancelButton());

        p.openInventory(inv);
    }


    // ============================================================
    // 📌 CONFIRMAR INVENTARIO COMPLETO (bulk)
    // ============================================================
    public static void openBulk(Player p, ItemStack[] items, double price) {

        AHConfirmHolder holder = new AHConfirmHolder(items, price);
        Inventory inv = Bukkit.createInventory(holder, 54, TITLE);

        fillBackground(inv);

        int slot = 10;

        for (ItemStack it : items) {
            if (it != null && !it.getType().isAir()) {

                if (slot == 17 || slot == 26 || slot == 35)
                    slot += 2;

                inv.setItem(slot++, previewItem(it.clone()));
            }
        }

        inv.setItem(48, cancelButton());
        inv.setItem(49, confirmButton());
        inv.setItem(50, pricePaper(price));

        p.openInventory(inv);
    }


    // ============================================================
    // 📌 BOTONES
    // ============================================================

    private static ItemStack confirmButton() {
        ItemStack i = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName("§a✔ Confirmar Venta");
        MetaUtil.setTag(m, "btn", "confirm");
        i.setItemMeta(m);
        return i;
    }

    private static ItemStack cancelButton() {
        ItemStack i = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName("§c✘ Cancelar");
        MetaUtil.setTag(m, "btn", "cancel");
        i.setItemMeta(m);
        return i;
    }

    private static ItemStack pricePaper(double price) {
        ItemStack i = new ItemStack(Material.PAPER);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName("§e💲 Precio: §a$" + price);
        MetaUtil.setTag(m, "btn", "price");
        i.setItemMeta(m);
        return i;
    }

    private static ItemStack previewItem(ItemStack base) {
        ItemStack i = base.clone();
        ItemMeta m = i.getItemMeta();
        MetaUtil.setTag(m, "preview", "1");
        i.setItemMeta(m);
        return i;
    }


    // ============================================================
    // 📌 FONDO
    // ============================================================
    private static void fillBackground(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = glass.getItemMeta();
        m.setDisplayName(" ");
        glass.setItemMeta(m);

        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null)
                inv.setItem(i, glass);
        }
    }
}
