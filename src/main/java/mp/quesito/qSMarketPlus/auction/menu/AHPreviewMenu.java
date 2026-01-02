package mp.quesito.qSMarketPlus.auction.menu;

import mp.quesito.qSMarketPlus.auction.AuctionItem;
import mp.quesito.qSMarketPlus.auction.holder.AHPreviewHolder;
import mp.quesito.qSMarketPlus.utils.MetaUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AHPreviewMenu {

    private static final String TITLE = "Contenido de Subasta";

    public static void open(Player player, AuctionItem auction) {

        AHPreviewHolder holder = new AHPreviewHolder(auction);
        Inventory inv = Bukkit.createInventory(holder, 54, TITLE);

        fillBackground(inv);

        // Mostrar todos los ítems del container
        int slot = 0;
        for (ItemStack i : auction.container) {
            if (i != null && !i.getType().isAir()) {
                inv.setItem(slot++, i.clone());
            }
        }

        // Botón volver
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName("§eVolver");
        MetaUtil.setTag(bm, "btn", "back");
        back.setItemMeta(bm);

        inv.setItem(53, back);

        player.openInventory(inv);
    }

    private static void fillBackground(Inventory inv) {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);

        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null)
                inv.setItem(i, glass);
        }
    }
}
