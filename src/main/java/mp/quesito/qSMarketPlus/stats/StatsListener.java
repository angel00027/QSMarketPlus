package mp.quesito.qSMarketPlus.stats;

import mp.quesito.qSMarketPlus.utils.MetaUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class StatsListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!(e.getInventory().getHolder() instanceof StatsHolder holder)) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        String btn = MetaUtil.getTag(meta, "btn");
        if (btn == null) return;

        switch (btn) {
            case "prev" -> StatsMenu.open(player, holder.getSubject(), holder.getPage() - 1);
            case "next" -> StatsMenu.open(player, holder.getSubject(), holder.getPage() + 1);
            case "close" -> player.closeInventory();
        }
    }
}