package mp.quesito.qSMarketPlus.auction.menu;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.auction.AuctionItem;
import mp.quesito.qSMarketPlus.auction.holder.AHExpiredHolder;
import mp.quesito.qSMarketPlus.utils.MenuItems;
import mp.quesito.qSMarketPlus.utils.MetaUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class AHExpiredMenu {

    public static void open(Player p, int page) {

        List<AuctionItem> expired = QSMarketPlus.getInstance()
                .getAuctionManager()
                .getExpired(p.getUniqueId());

        // Max page calculated based on a 28-slot inner content grid
        int maxPage = Math.max(1, (int) Math.ceil(expired.size() / 28.0));
        page = Math.max(1, Math.min(page, maxPage));

        Inventory inv = Bukkit.createInventory(
                new AHExpiredHolder(p, page, expired),
                54,
                "✦ Subastas Expiradas ✦"
        );

        int start = (page - 1) * 28;
        int end = Math.min(start + 28, expired.size());

        int slot = 10;

        for (int i = start; i < end; i++) {
            AuctionItem auc = expired.get(i);
            ItemStack icon;

            // Handle bulk packaging display
            if (auc.isBulk() && auc.container != null) {
                icon = new ItemStack(Material.CHEST);
                ItemMeta m = icon.getItemMeta();
                if (m != null) {
                    // Mejora: Cuenta ítems reales en lugar de ranuras vacías del cofre
                    int totalItems = 0;
                    for (ItemStack stack : auc.container) {
                        if (stack != null && !stack.getType().isAir()) {
                            totalItems += stack.getAmount();
                        }
                    }
                    m.setDisplayName("§ePack Expirado (" + totalItems + " items)");
                    icon.setItemMeta(m);
                }
            } else if (auc.item != null) {
                icon = auc.item.clone();
            } else {
                continue; // Skip corrupted database entries
            }

            ItemMeta m = icon.getItemMeta();
            if (m != null) {
                m.getPersistentDataContainer().set(
                        MetaUtil.key("expired_index"),
                        PersistentDataType.INTEGER,
                        i
                );
                icon.setItemMeta(m);
            }

            inv.setItem(slot, icon);

            slot++;
            if (slot % 9 == 8) slot += 2;
        }

        // -------------------------------
        // NAVIGATION BUTTONS (Con limpieza)
        // -------------------------------
        if (page > 1) {
            ItemStack prev = MenuItems.customHead(
                    "<yellow>Anterior",
                    "http://textures.minecraft.net/texture/6e8c47367c3e94312bc7"
            );
            if (prev != null && prev.getItemMeta() != null) {
                ItemMeta meta = prev.getItemMeta();
                MetaUtil.setTag(meta, "btn", "prev");
                prev.setItemMeta(meta);
                inv.setItem(45, prev);
            }
        } else {
            inv.setItem(45, null); // Forzar limpieza por cambios de página rápidos
        }

        if (page < maxPage) {
            ItemStack next = MenuItems.customHead(
                    "<yellow>Siguiente",
                    "http://textures.minecraft.net/texture/555e1f985a0c4c3f98"
            );
            if (next != null && next.getItemMeta() != null) {
                ItemMeta meta = next.getItemMeta();
                MetaUtil.setTag(meta, "btn", "next");
                next.setItemMeta(meta);
                inv.setItem(53, next);
            }
        } else {
            inv.setItem(53, null); // Forzar limpieza
        }

        // -------------------------------
        // CLOSE BUTTON
        // -------------------------------
        ItemStack close = MenuItems.red("close", "§cCerrar");
        if (close != null && close.getItemMeta() != null) {
            ItemMeta meta = close.getItemMeta();
            MetaUtil.setTag(meta, "btn", "close");
            close.setItemMeta(meta);
            inv.setItem(49, close);
        }

        p.openInventory(inv);
    }
}