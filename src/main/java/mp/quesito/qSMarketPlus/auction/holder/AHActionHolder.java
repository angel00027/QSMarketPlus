package mp.quesito.qSMarketPlus.auction.holder;

import mp.quesito.qSMarketPlus.auction.AuctionItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class AHActionHolder implements InventoryHolder {

    private final Player viewer;
    private final AuctionItem auction;
    private final boolean isOwner;
    private final int page;

    private final Inventory inv;

    public AHActionHolder(Player viewer, AuctionItem auction, boolean isOwner, int page, Inventory inv) {
        this.viewer = viewer;
        this.auction = auction;
        this.isOwner = isOwner;
        this.page = page;
        this.inv = inv;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public Player getViewer() {
        return viewer;
    }

    public AuctionItem getAuction() {
        return auction;
    }

    public boolean isOwner() {
        return isOwner;
    }

    public int getPage() {
        return page;
    }
}
