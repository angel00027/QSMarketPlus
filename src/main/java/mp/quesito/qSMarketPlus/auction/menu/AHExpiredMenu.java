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

            if (auc.isBulk()) {
                icon = new ItemStack(Material.CHEST);
                ItemMeta m = icon.getItemMeta();
                m.setDisplayName("§ePack Expirado (" + auc.container.length + " items)");
                icon.setItemMeta(m);
            } else {
                icon = auc.item.clone();
            }

            ItemMeta m = icon.getItemMeta();
            m.getPersistentDataContainer().set(
                    MetaUtil.key("expired_index"),
                    PersistentDataType.INTEGER,
                    i
            );
            icon.setItemMeta(m);

            inv.setItem(slot, icon);

            slot++;
            if (slot % 9 == 8) slot += 2;
        }

        // -------------------------------
        // BOTONES DE NAVEGACIÓN
        // -------------------------------
        if (page > 1) {
            ItemStack prev = MenuItems.customHead(
                    "<yellow>Anterior",
                    "http://textures.minecraft.net/texture/6e8c47367c3e94312bc7"
            );
            MetaUtil.setTag(prev.getItemMeta(), "btn", "prev");
            inv.setItem(45, prev);
        }

        if (page < maxPage) {
            ItemStack next = MenuItems.customHead(
                    "<yellow>Siguiente",
                    "http://textures.minecraft.net/texture/555e1f985a0c4c3f98"
            );
            MetaUtil.setTag(next.getItemMeta(), "btn", "next");
            inv.setItem(53, next);
        }

        // -------------------------------
        // BOTÓN CERRAR
        // -------------------------------
        ItemStack close = MenuItems.red("close", "§cCerrar");
        MetaUtil.setTag(close.getItemMeta(), "btn", "close");
        inv.setItem(49, close); // lo ponemos al centro debajo de los items

        p.openInventory(inv);
    }

}
