package mp.quesito.qSMarketPlus.auction;

import org.bukkit.inventory.ItemStack;
import java.util.UUID;

public class AuctionItem {

    public enum Status {
        ACTIVE,
        SOLD,
        CANCELLED,
        EXPIRED,
        EXPIRED_TAKEN
    }

    public int id = -1;          // ← id de la fila en SQL

    public UUID seller;
    public UUID buyer;
    public double price;
    public ItemStack item;
    public ItemStack[] container;

    public long created;
    public long expiresAt;
    public Status status;

    // Constructor: item individual
    public AuctionItem(UUID seller, ItemStack item, double price, long created, long expiresAt) {
        this.seller = seller;
        this.item = item;
        this.price = price;
        this.created = created;
        this.expiresAt = expiresAt;
        this.status = Status.ACTIVE;
    }

    // Constructor: inventario completo
    public AuctionItem(UUID seller, ItemStack[] items, double price, long created, long expiresAt) {
        this.seller = seller;
        this.container = items;
        this.price = price;
        this.created = created;
        this.expiresAt = expiresAt;
        this.status = Status.ACTIVE;
    }

    public boolean isBulk() {
        return container != null;
    }

    public boolean isExpired() {
        return status == Status.EXPIRED;
    }

    public boolean isCancelled() {
        return status == Status.CANCELLED;
    }

    public boolean isSold() {
        return status == Status.SOLD;
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    public String getReadableName() {

        // Subasta individual
        if (!isBulk()) {
            return item.getAmount() + "x " + item.getType().name();
        }

        // Subasta masiva
        int total = 0;
        for (ItemStack it : container) {
            if (it != null && !it.getType().isAir()) {
                total += it.getAmount();
            }
        }

        return total + " items en lote";
    }

}
