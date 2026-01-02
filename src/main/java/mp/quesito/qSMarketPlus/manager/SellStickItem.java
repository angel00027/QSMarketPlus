package mp.quesito.qSMarketPlus.manager;

import mp.quesito.qSMarketPlus.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class SellStickItem {

    public static ItemStack getSellStick() {
        ItemStack stick = new ItemStack(Material.STICK);
        ItemMeta meta = stick.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6Palo de Venta");
            meta.setLore(List.of(
                    "§7Shift-Click sobre un cofre",
                    "§7para vender todos los ítems dentro"
            ));
            stick.setItemMeta(meta);
        }
        return stick;
    }

    public static boolean isSellStick(ItemStack item) {
        if (item == null) return false;
        if (item.getType() != Material.STICK) return false;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
        return item.getItemMeta().getDisplayName().equals("§6Palo de Venta");
    }

    public static void give(Player player) {
        player.getInventory().addItem(getSellStick());
        MessageUtil.msg(player, "<green>¡Has recibido el Palo de Venta!");
    }
}
