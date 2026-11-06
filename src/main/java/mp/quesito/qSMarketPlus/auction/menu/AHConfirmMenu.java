package mp.quesito.qSMarketPlus.auction.menu;

import mp.quesito.qSMarketPlus.auction.holder.AHConfirmHolder;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import mp.quesito.qSMarketPlus.utils.MetaUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class AHConfirmMenu {

    /** Huecos del pack: 5 filas completas. */
    public static final int PACK_SLOTS = 45;

    // ============================================================
    // CONFIRMAR ITEM UNICO
    // ============================================================
    public static void openSingle(Player p, ItemStack original, double price, int slot) {

        ItemStack backup = original.clone();
        AHConfirmHolder holder = new AHConfirmHolder(original.clone(), price, backup, slot);

        Inventory inv = Bukkit.createInventory(holder, 27, AHUi.title("confirm", "Confirmar Subasta"));

        inv.setItem(11, previewItem(original.clone()));
        inv.setItem(13, priceTag(price));
        inv.setItem(15, AHUi.button("confirm", Material.LIME_STAINED_GLASS_PANE, "§a✔ Confirmar Venta", "btn", "confirm"));
        inv.setItem(22, AHUi.button("cancel", Material.RED_STAINED_GLASS_PANE, "§c✘ Cancelar", "btn", "cancel"));

        AHUi.fill(inv);
        p.openInventory(inv);
    }

    // ============================================================
    // CONFIRMAR INVENTARIO COMPLETO (pack)
    // ============================================================
    public static void openBulk(Player p, ItemStack[] items, double price) {

        AHConfirmHolder holder = new AHConfirmHolder(items, price);
        Inventory inv = Bukkit.createInventory(holder, 54, AHUi.title("confirm-bulk", "Confirmar Subasta"));

        int slot = 0;
        for (ItemStack it : items) {
            if (it != null && !it.getType().isAir() && slot < PACK_SLOTS) {
                inv.setItem(slot++, previewItem(it.clone()));
            }
        }

        inv.setItem(48, AHUi.button("cancel", Material.RED_STAINED_GLASS_PANE, "§c✘ Cancelar", "btn", "cancel"));
        inv.setItem(49, AHUi.button("confirm", Material.LIME_STAINED_GLASS_PANE, "§a✔ Confirmar Venta", "btn", "confirm"));
        inv.setItem(50, priceTag(price));

        AHUi.fill(inv);
        p.openInventory(inv);
    }

    private static ItemStack priceTag(double price) {
        ItemStack tag = AHUi.button("price", Material.PAPER, "Precio", "btn", "price");
        ItemMeta m = tag.getItemMeta();
        m.setDisplayName(MessageUtil.toLegacy(
                "<#e8c97a>Precio  <gradient:#ffe08a:#ffb347>$" + AHUi.money(price) + "</gradient>"));
        m.setLore(AHUi.lore("price", List.of("<#8a7a6a>Lo que pagará el comprador")));
        tag.setItemMeta(m);
        return tag;
    }

    private static ItemStack previewItem(ItemStack base) {
        ItemStack i = base.clone();
        ItemMeta m = i.getItemMeta();
        MetaUtil.setTag(m, "preview", "1");
        i.setItemMeta(m);
        return i;
    }
}
