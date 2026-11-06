package mp.quesito.qSMarketPlus.pwarp.holder;

import mp.quesito.qSMarketPlus.pwarp.Pwarp;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class PwarpActionHolder implements InventoryHolder {

    private final Player player;
    private final Pwarp warp;
    private final int page;
    private final PwarpHolder.Filter filter;

    public PwarpActionHolder(Player player, Pwarp warp, int page, PwarpHolder.Filter filter) {
        this.player = player;
        this.warp = warp;
        this.page = page;
        this.filter = filter;
    }

    public Player getPlayer() { return player; }
    public Pwarp getWarp() { return warp; }
    public int getPage() { return page; }
    public PwarpHolder.Filter getFilter() { return filter; }

    @Override
    public Inventory getInventory() { return null; }
}