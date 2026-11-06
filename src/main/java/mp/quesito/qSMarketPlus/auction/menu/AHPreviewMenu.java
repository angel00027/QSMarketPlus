package mp.quesito.qSMarketPlus.auction.menu;

import mp.quesito.qSMarketPlus.auction.AuctionItem;
import mp.quesito.qSMarketPlus.auction.holder.AHPreviewHolder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class AHPreviewMenu {

    public static void open(Player player, AuctionItem auction) {

        AHPreviewHolder holder = new AHPreviewHolder(auction);
        Inventory inv = Bukkit.createInventory(holder, 54, AHUi.title("content", "Contenido de Subasta"));

        // Mismo marco que la confirmacion del pack: 5 filas de objetos
        int slot = 0;
        if (auction.container != null) {
            for (ItemStack i : auction.container) {
                if (i != null && !i.getType().isAir() && slot < AHConfirmMenu.PACK_SLOTS) {
                    inv.setItem(slot++, i.clone());
                }
            }
        }

        inv.setItem(49, AHUi.button("back", Material.ARROW, "§eVolver", "btn", "back"));

        AHUi.fill(inv);
        player.openInventory(inv);
    }
}
