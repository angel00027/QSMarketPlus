package mp.quesito.qSMarketPlus.listeners;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.manager.ChestSellManager;
import mp.quesito.qSMarketPlus.manager.SellStickItem;
import mp.quesito.qSMarketPlus.shop.PlayerShop;
import mp.quesito.qSMarketPlus.utils.WorldGuardUtils;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class SellStickListener implements Listener {

    private final ChestSellManager sellManager;

    public SellStickListener(QSMarketPlus plugin) {
        this.sellManager = new ChestSellManager(plugin);
    }

    @EventHandler
    public void onPlayerUseSellStick(PlayerInteractEvent e) {
        // Solo Shift + Left Click
        if (e.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Player player = e.getPlayer();
        if (!player.isSneaking()) return;

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!SellStickItem.isSellStick(hand)) return;

        Block clickedBlock = e.getClickedBlock();
        if (clickedBlock == null) return;

        if (!(clickedBlock.getState() instanceof Chest chest)) {
            player.sendMessage("§cDebes hacer click en un cofre válido.");
            return;
        }

        e.setCancelled(true); // evita abrir el cofre

        // Revisar si es PlayerShop
        PlayerShop shop = QSMarketPlus.getInstance().getShopManager().getShopAtLocation(clickedBlock.getLocation());
        if (shop != null) {
            if (!shop.getOwner().equals(player.getUniqueId())) {
                player.sendMessage("§c¡No puedes vender ítems de la tienda de otro jugador!");
                return;
            }
        }

        // Verificar WorldGuard
        if (!WorldGuardUtils.canBuild(player, clickedBlock.getLocation())) {
            player.sendMessage("§cNo puedes vender aquí, no tienes permisos en esta región.");
            return;
        }

        Inventory chestInv = chest.getInventory();
        if (chestInv == null || chestInv.getContents().length == 0) {
            player.sendMessage("§cEl cofre está vacío.");
            return;
        }

        // Vender inventario del cofre
        sellManager.sellChestInventory(player, chestInv);
    }

}
