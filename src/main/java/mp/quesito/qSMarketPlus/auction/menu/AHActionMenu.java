package mp.quesito.qSMarketPlus.auction.menu;

import mp.quesito.qSMarketPlus.auction.AuctionItem;
import mp.quesito.qSMarketPlus.auction.holder.AHActionHolder;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class AHActionMenu {

    private static final int SIZE = 27;

    public static void open(Player player, AuctionItem auc, int page) {

        boolean isOwner = auc.seller.equals(player.getUniqueId());
        String title = AHUi.title("options", "<gold>Opciones de Subasta");

        Inventory inv = Bukkit.createInventory(null, SIZE, title);
        AHActionHolder holder = new AHActionHolder(player, auc, isOwner, page, inv);
        inv = Bukkit.createInventory(holder, SIZE, title);

        inv.setItem(4, summary(auc));

        if (auc.isBulk()) {
            inv.setItem(11, AHUi.button("view", Material.CHEST, "<yellow>Ver Contenido", "action", "view"));
        }

        if (isOwner) {
            inv.setItem(13, AHUi.button("cancel-auction", Material.BARRIER, "<red>Cancelar Subasta", "action", "cancel"));
        } else {
            inv.setItem(13, AHUi.button("buy", Material.EMERALD, "<green>Comprar", "action", "buy"));
        }

        inv.setItem(15, AHUi.button("back", Material.ARROW, "<yellow>Volver", "action", "back"));

        AHUi.fill(inv);
        player.openInventory(inv);
    }

    /** Lo que se esta comprando, arriba en el centro. */
    private static ItemStack summary(AuctionItem auc) {

        ItemStack shown;
        if (auc.isBulk()) {
            shown = new ItemStack(Material.CHEST);
        } else if (auc.item != null && !auc.item.getType().isAir()) {
            shown = auc.item.clone();
        } else {
            shown = new ItemStack(Material.BARRIER);
        }

        ItemMeta meta = shown.getItemMeta();
        List<String> lore = new ArrayList<>(meta.hasLore() ? meta.getLore() : List.of());
        if (!lore.isEmpty()) lore.add("");
        lore.addAll(AHUi.lore("summary", List.of(
                "<#5a4a3a>━━━━━━━━━━━━━━━━━━",
                "<#e8c97a>Vendedor  <white><seller>",
                "<#e8c97a>Precio  <gradient:#ffe08a:#ffb347>$<price></gradient>",
                "<#e8c97a>Termina en  <white><time>"
        ), "seller", AHUi.seller(auc), "price", AHUi.money(auc.price), "time", AHUi.timeLeft(auc.expiresAt)));
        meta.setLore(lore);
        shown.setItemMeta(meta);
        return shown;
    }
}
