package mp.quesito.qSMarketPlus.trade;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class TradeRecord {

    public long id;

    public final UUID playerA;
    public final UUID playerB;

    public final List<ItemStack> itemsA;
    public final List<ItemStack> itemsB;

    public final double moneyA;
    public final double moneyB;
    public final int xpA;
    public final int xpB;

    public final long timestamp;

    public TradeRecord(UUID playerA, UUID playerB,
                       List<ItemStack> itemsA, List<ItemStack> itemsB,
                       double moneyA, double moneyB, int xpA, int xpB, long timestamp) {
        this.playerA = playerA;
        this.playerB = playerB;
        this.itemsA = itemsA;
        this.itemsB = itemsB;
        this.moneyA = moneyA;
        this.moneyB = moneyB;
        this.xpA = xpA;
        this.xpB = xpB;
        this.timestamp = timestamp;
    }
}