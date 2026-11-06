package mp.quesito.qSMarketPlus.stats;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class StatsHolder implements InventoryHolder {

    private final UUID subject;
    private int page;
    private Inventory inventory;

    public StatsHolder(UUID subject, int page) {
        this.subject = subject;
        this.page = page;
    }

    public UUID getSubject() {
        return subject;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}