package mp.quesito.qSMarketPlus.auction.menu;

import mp.quesito.qSMarketPlus.auction.AuctionItem;
import mp.quesito.qSMarketPlus.auction.holder.AHActionHolder;
import mp.quesito.qSMarketPlus.utils.MenuItems;
import mp.quesito.qSMarketPlus.utils.MetaUtil;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AHActionMenu {

    private static final int SIZE = 27;
    private static final String TITLE = MenuItems.miniToLegacy("<gold>Opciones de Subasta");

    public static void open(Player player, AuctionItem auc, int page) {

        boolean isOwner = auc.seller.equals(player.getUniqueId());

        Inventory inv = Bukkit.createInventory(null, SIZE, TITLE);
        AHActionHolder holder = new AHActionHolder(player, auc, isOwner, page, inv);

        inv = Bukkit.createInventory(holder, SIZE, TITLE);

        fill(inv);

        if (auc.isBulk()) {
            inv.setItem(11, button(Material.CHEST, "<yellow>Ver Contenido", "view"));
        }

        if (isOwner) {
            inv.setItem(13, button(Material.BARRIER, "<red>Cancelar Subasta", "cancel"));
        } else {
            inv.setItem(13, button(Material.EMERALD, "<green>Comprar", "buy"));
        }

        inv.setItem(15, button(Material.ARROW, "<yellow>Volver", "back"));

        player.openInventory(inv);
    }

    private static void fill(Inventory inv) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);

        for (int i = 0; i < inv.getSize(); i++)
            inv.setItem(i, filler);
    }

    private static ItemStack button(Material mat, String name, String tag) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MenuItems.miniToLegacy(name));
        MetaUtil.setTag(meta, "action", tag);
        item.setItemMeta(meta);
        return item;
    }
}
