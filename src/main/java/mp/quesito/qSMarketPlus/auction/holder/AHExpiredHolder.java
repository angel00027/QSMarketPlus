package mp.quesito.qSMarketPlus.auction.holder;

import mp.quesito.qSMarketPlus.auction.AuctionItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

public class AHExpiredHolder implements InventoryHolder {

    private final Player player;
    private final int page;
    private final List<AuctionItem> items;

    public AHExpiredHolder(Player player, int page, List<AuctionItem> items) {
        this.player = player;
        this.page = page;
        this.items = items;
    }

    public List<AuctionItem> getItems() { return items; }
    public int getPage() { return page; }

    @Override
    public Inventory getInventory() { return null; }
}
