package mp.quesito.qSMarketPlus.pwarp.holder;

import mp.quesito.qSMarketPlus.pwarp.Pwarp;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

public class PwarpHolder implements InventoryHolder {

    public enum Filter {
        ALL,
        FAVORITES
    }

    private final Player player;
    private final int page;
    private final Filter filter;
    private final List<Pwarp> pageItems;

    public PwarpHolder(Player player, int page, Filter filter, List<Pwarp> pageItems) {
        this.player = player;
        this.page = page;
        this.filter = filter;
        this.pageItems = pageItems;
    }

    public Player getPlayer() { return player; }
    public int getPage() { return page; }
    public Filter getFilter() { return filter; }
    public List<Pwarp> getPageItems() { return pageItems; }

    @Override
    public Inventory getInventory() { return null; }
}