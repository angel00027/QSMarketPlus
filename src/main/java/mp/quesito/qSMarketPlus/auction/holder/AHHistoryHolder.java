package mp.quesito.qSMarketPlus.auction.holder;

import mp.quesito.qSMarketPlus.auction.AuctionItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;
import java.util.UUID;

public class AHHistoryHolder implements InventoryHolder {

    private Inventory inv;
    private final Player viewer;
    private final UUID subject;
    private final List<AuctionItem> items;
    private final int page;

    public AHHistoryHolder(Player viewer, UUID subject, List<AuctionItem> items, int page) {
        this.viewer = viewer;
        this.subject = subject;
        this.items = items;
        this.page = page;
    }

    public void setInventory(Inventory inv) {
        this.inv = inv;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public Player getViewer() {
        return viewer;
    }

    public UUID getSubject() {
        return subject;
    }

    public List<AuctionItem> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }
}
