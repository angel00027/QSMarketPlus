package mp.quesito.qSMarketPlus.auction.holder;

import mp.quesito.qSMarketPlus.auction.AuctionItem;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class AHPreviewHolder implements InventoryHolder {

    private final AuctionItem auction;

    public AHPreviewHolder(AuctionItem auction) {
        this.auction = auction;
    }

    public AuctionItem getAuction() {
        return auction;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
