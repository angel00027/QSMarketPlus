package mp.quesito.qSMarketPlus.auction.holder;

import mp.quesito.qSMarketPlus.auction.AuctionItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;
import java.util.UUID;

public class AHExpiredHolder implements InventoryHolder {

    private final Player player;
    private final UUID subject;
    private final int page;
    private final List<AuctionItem> items;

    public AHExpiredHolder(Player player, UUID subject, int page, List<AuctionItem> items) {
        this.player = player;
        this.subject = subject;
        this.page = page;
        this.items = items;
    }

    public List<AuctionItem> getItems() { return items; }
    public int getPage() { return page; }
    public UUID getSubject() { return subject; }

    @Override
    public Inventory getInventory() { return null; }
}
