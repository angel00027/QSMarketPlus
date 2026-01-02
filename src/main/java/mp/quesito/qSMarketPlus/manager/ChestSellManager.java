package mp.quesito.qSMarketPlus.manager;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.shop.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ChestSellManager {

    private final QSMarketPlus plugin;

    public ChestSellManager(QSMarketPlus plugin) {
        this.plugin = plugin;
    }

    public void sellChestInventory(Player player, Inventory chestInv) {

        double totalMoney = 0;
        ItemManager itemManager = plugin.getItemManager();

        for (int i = 0; i < chestInv.getSize(); i++) {
            ItemStack item = chestInv.getItem(i);
            if (item == null || item.getType().isAir()) continue;

            ShopItem shopItem = itemManager.getItemById(item.getType().name().toLowerCase());

            // Si no está en la config, ignorar
            if (shopItem == null) continue;

            // Sumar dinero
            totalMoney += shopItem.getSell() * item.getAmount();

            // Ejecutar sellCommands
            shopItem.executeSellCommands(player);

            // Eliminar ítem del cofre
            chestInv.setItem(i, null);
        }

        if (totalMoney > 0) {
            QSMarketPlus.economy.depositPlayer(player, totalMoney);
            player.sendMessage("§a¡Has vendido todo el cofre por §f" + totalMoney + " §amonedas!");
        } else {
            player.sendMessage("§cNo hay items vendibles en este cofre.");
        }

        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
    }
}
