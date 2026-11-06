package mp.quesito.qSMarketPlus.listeners;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.pwarp.Pwarp;
import mp.quesito.qSMarketPlus.pwarp.PwarpManager;
import mp.quesito.qSMarketPlus.pwarp.holder.PwarpActionHolder;
import mp.quesito.qSMarketPlus.pwarp.holder.PwarpHolder;
import mp.quesito.qSMarketPlus.pwarp.menu.PwarpActionMenu;
import mp.quesito.qSMarketPlus.pwarp.menu.PwarpMenu;
import mp.quesito.qSMarketPlus.utils.Lang;
import mp.quesito.qSMarketPlus.utils.MetaUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class PwarpListener implements Listener {

    private final PwarpManager manager = QSMarketPlus.getInstance().getPwarpManager();

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory().getHolder() == null) return;
        if (!(e.getView().getTopInventory().getHolder() instanceof PwarpHolder holder)) {
            if (e.getView().getTopInventory().getHolder() instanceof PwarpActionHolder action) {
                handleActionClick(e, action);
            }
            return;
        }
        e.setCancelled(true);

        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) return;

        ItemStack item = e.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        String btn = MetaUtil.getTag(item.getItemMeta(), "btn");
        if (btn == null) return;

        Player player = (Player) e.getWhoClicked();
        int page = holder.getPage();
        PwarpHolder.Filter filter = holder.getFilter();

        switch (btn) {
            case "prev" -> PwarpMenu.open(player, page - 1, filter);
            case "next" -> PwarpMenu.open(player, page + 1, filter);
            case "filter" -> PwarpMenu.open(player, 1,
                    filter == PwarpHolder.Filter.ALL ? PwarpHolder.Filter.FAVORITES : PwarpHolder.Filter.ALL);
            case "close" -> player.closeInventory();
            case "warp" -> {
                String warpName = MetaUtil.getTag(item.getItemMeta(), "pwarp");
                Pwarp warp = manager.get(warpName);
                if (warp == null) {
                    Lang.msg(player, "pwarp.not-found", "warp", warpName == null ? "?" : warpName);
                    PwarpMenu.open(player, 1, filter);
                    return;
                }
                PwarpActionMenu.open(player, warp, page, filter);
            }
        }
    }

    private void handleActionClick(InventoryClickEvent e, PwarpActionHolder holder) {
        e.setCancelled(true);

        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) return;

        ItemStack item = e.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;

        String btn = MetaUtil.getTag(item.getItemMeta(), "btn");
        if (btn == null) return;

        Player player = holder.getPlayer();
        Pwarp warp = holder.getWarp();
        int page = holder.getPage();
        PwarpHolder.Filter filter = holder.getFilter();

        switch (btn) {
            case "back" -> PwarpMenu.open(player, page, filter);
            case "teleport" -> {
                if (warp == null || manager.get(warp.getName()) == null) {
                    Lang.msg(player, "pwarp.not-found", "warp", warp == null ? "?" : warp.getDisplayName());
                    PwarpMenu.open(player, page, filter);
                    return;
                }
                if (!manager.teleport(player, warp)) {
                    Lang.msg(player, "pwarp.not-safe");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                Lang.msg(player, "pwarp.teleported", "owner", warp.getOwnerName(), "warp", warp.getDisplayName());
            }
            case "fav" -> {
                boolean added = manager.toggleFavorite(player.getUniqueId(), warp.getName());
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
                Lang.msg(player, added ? "pwarp.fav-added" : "pwarp.fav-removed", "warp", warp.getDisplayName());
                PwarpActionMenu.open(player, warp, page, filter);
            }
        }
    }
}