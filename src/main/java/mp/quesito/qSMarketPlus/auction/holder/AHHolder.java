package mp.quesito.qSMarketPlus.auction.holder;

import mp.quesito.qSMarketPlus.auction.AuctionItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.*;

public class AHHolder implements InventoryHolder {

    public enum FilterMode {
        NONE,
        PRICE_ASC,
        PRICE_DESC,
        ONLY_ITEMS,
        ONLY_BULK
    }

    private static final Map<UUID, FilterMode> playerFilters = new HashMap<>();

    public static FilterMode getFilter(Player player) {
        return playerFilters.getOrDefault(player.getUniqueId(), FilterMode.NONE);
    }

    public static void setFilter(Player player, FilterMode mode) {
        playerFilters.put(player.getUniqueId(), mode);
    }

    private Inventory inventory;              // <-- YA NO es final
    private final List<AuctionItem> pageItems;
    private final int page;
    private final Player viewer;

    public AHHolder(Player viewer, List<AuctionItem> pageItems, int page) {
        this.viewer = viewer;
        this.pageItems = pageItems;
        this.page = Math.max(1, page);

        playerFilters.putIfAbsent(viewer.getUniqueId(), FilterMode.NONE);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    // NUEVO: método para asignar inventario una vez creado
    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public List<AuctionItem> getPageItems() {
        return pageItems;
    }

    public int getPage() {
        return page;
    }

    public Player getViewer() {
        return viewer;
    }

    public FilterMode getFilterMode() {
        return getFilter(viewer);
    }
}
