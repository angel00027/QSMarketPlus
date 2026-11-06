package mp.quesito.qSMarketPlus.trade;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

public class TradeHistoryHolder implements InventoryHolder {

    private Inventory inv;
    private final Player viewer;
    private final List<TradeRecord> records;
    private final int page;

    public TradeHistoryHolder(Player viewer, List<TradeRecord> records, int page) {
        this.viewer = viewer;
        this.records = records;
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

    public List<TradeRecord> getRecords() {
        return records;
    }

    public int getPage() {
        return page;
    }
}