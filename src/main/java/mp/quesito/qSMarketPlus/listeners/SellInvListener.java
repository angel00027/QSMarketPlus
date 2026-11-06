package mp.quesito.qSMarketPlus.listeners;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.commands.SellCommand;
import mp.quesito.qSMarketPlus.holder.SellInvHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class SellInvListener implements Listener {

    private final QSMarketPlus plugin;

    public SellInvListener(QSMarketPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof SellInvHolder)) return;
        if (!(e.getPlayer() instanceof Player p)) return;

        plugin.getServer().getScheduler().runTask(plugin, () ->
                new SellCommand(plugin, plugin.getCategoryManager(), plugin.getItemManager())
                        .sellInvOnClose(p, e.getInventory())
        );
    }
}