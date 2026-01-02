package mp.quesito.qSMarketPlus.listeners;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.manager.SignShopManager;
import mp.quesito.qSMarketPlus.shop.SignShop;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class SignShopListener implements Listener {

    private final SignShopManager manager = QSMarketPlus.getInstance().getSignShopManager();

    @EventHandler
    public void onClick(PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign)) return;

        SignShop shop = manager.getShopAt(block.getLocation());
        if (shop == null) return;

        Player player = e.getPlayer();
        e.setCancelled(true); // Cancelar interacción normal

        Action action = e.getAction();

        // --- Shift + clic derecho → romper cartel ---
        if (action == Action.RIGHT_CLICK_BLOCK && player.isSneaking()) {
            boolean canBreak = player.hasPermission("qsmarket.admin") ||
                    (shop.getOwnerUUID() != null && shop.getOwnerUUID().equals(player.getUniqueId().toString()));
            if (canBreak) {
                manager.removeShop(shop);
                block.breakNaturally();
                player.sendMessage("§aTienda eliminada correctamente.");
            } else {
                player.sendMessage("§cNo tienes permiso para romper esta tienda.");
            }
            return;
        }

        // --- Clic izquierdo normal → comprar ---
        if (action == Action.LEFT_CLICK_BLOCK && !player.isSneaking()) {
            shop.buy(player);
            return;
        }

        // --- Shift + clic izquierdo → vender cantidad definida ---
        if (action == Action.LEFT_CLICK_BLOCK && player.isSneaking()) {
            shop.sell(player);
            return;
        }

        // --- Clic derecho normal → vender todo ---
        if (action == Action.RIGHT_CLICK_BLOCK && !player.isSneaking()) {
            shop.sellAll(player);
        }
    }
}
