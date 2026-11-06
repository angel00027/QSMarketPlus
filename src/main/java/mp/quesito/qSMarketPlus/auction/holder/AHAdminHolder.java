package mp.quesito.qSMarketPlus.auction.holder;

import mp.quesito.qSMarketPlus.auction.AuctionItem;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class AHAdminHolder implements InventoryHolder {

    private final AuctionItem auction;
    private final String source;   // "ah", "history" o "expired"
    private final UUID subject;    // jugador cuyo historial/expiradas se está viendo
    private final int page;

    public AHAdminHolder(AuctionItem auction, String source, UUID subject, int page) {
        this.auction = auction;
        this.source = source;
        this.subject = subject;
        this.page = page;
    }

    public AuctionItem getAuction() {
        return auction;
    }

    public String getSource() {
        return source;
    }

    public UUID getSubject() {
        return subject;
    }

    public int getPage() {
        return page;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}