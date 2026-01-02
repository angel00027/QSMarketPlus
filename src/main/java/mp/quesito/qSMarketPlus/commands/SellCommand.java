package mp.quesito.qSMarketPlus.commands;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.manager.CategoryManager;
import mp.quesito.qSMarketPlus.manager.ItemManager;
import mp.quesito.qSMarketPlus.shop.ShopCategory;
import mp.quesito.qSMarketPlus.shop.ShopItem;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class SellCommand implements CommandExecutor {

    private final QSMarketPlus plugin;
    private final CategoryManager categoryManager;
    private final ItemManager itemManager;

    public SellCommand(QSMarketPlus plugin, CategoryManager categoryManager, ItemManager itemManager) {
        this.plugin = plugin;
        this.categoryManager = categoryManager;
        this.itemManager = itemManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Solo jugadores");
            return true;
        }

        if (args.length == 0) {
            MessageUtil.msg(p,
                    "<yellow>Uso: <white>/sell hand</white>, <white>/sell similar</white>, <white>/sell all</white>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "hand" -> sellHand(p);
            case "similar" -> sellSimilar(p);
            case "all" -> sellAll(p);
            default -> MessageUtil.msg(p,
                    "<red>Comando inválido. Usa: <white>/sell hand</white>, <white>/sell similar</white>, <white>/sell all</white>");
        }

        return true;
    }

    private void sellHand(Player p) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            MessageUtil.msg(p, "<red>No tienes nada en la mano.");
            return;
        }

        ShopItem shopItem = findShopItem(hand);
        if (shopItem == null || shopItem.getSell() <= 0) {
            MessageUtil.msg(p, "<red>Este objeto no se puede vender.");
            return;
        }

        int amount = hand.getAmount();
        double total = shopItem.getSell() * amount;

        p.getInventory().setItemInMainHand(null);
        QSMarketPlus.economy.depositPlayer(p, total);
        shopItem.executeSellCommands(p);

        MessageUtil.msg(p,
                "<green>Vendiste <yellow>" + amount + "x " + shopItem.getName() +
                        "</yellow> por <white>$" + total);
    }

    private void sellSimilar(Player p) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            MessageUtil.msg(p, "<red>No tienes nada en la mano.");
            return;
        }

        ShopItem shopItem = findShopItem(hand);
        if (shopItem == null || shopItem.getSell() <= 0) {
            MessageUtil.msg(p, "<red>Este ítem no se puede vender.");
            return;
        }

        int totalItems = 0;
        for (ItemStack s : p.getInventory().getContents()) {
            if (s != null && s.isSimilar(shopItem.getRealItem())) totalItems += s.getAmount();
        }

        if (totalItems <= 0) {
            MessageUtil.msg(p, "<yellow>No tienes más ítems similares.");
            return;
        }

        double total = totalItems * shopItem.getSell();

        // Quitar todos los ítems similares
        for (ItemStack s : p.getInventory().getContents()) {
            if (s != null && s.isSimilar(shopItem.getRealItem())) {
                p.getInventory().removeItem(s);
            }
        }

        QSMarketPlus.economy.depositPlayer(p, total);
        shopItem.executeSellCommands(p);

        MessageUtil.msg(p,
                "<green>Vendiste todos tus <yellow>" + totalItems + "x " + shopItem.getName() +
                        "</yellow> por <white>$" + total);
    }

    private void sellAll(Player p) {
        double totalMoney = 0;

        for (ShopCategory cat : categoryManager.getCategories().values()) {
            itemManager.loadCategoryItems(cat);
            Map<String, ShopItem> items = itemManager.getItems(cat.getId());

            for (ShopItem shopItem : items.values()) {
                if (shopItem.getSell() <= 0) continue;

                for (ItemStack s : p.getInventory().getContents()) {
                    if (s != null && s.isSimilar(shopItem.getRealItem())) {
                        int count = s.getAmount();
                        totalMoney += count * shopItem.getSell();
                        p.getInventory().removeItem(s);
                        shopItem.executeSellCommands(p);
                    }
                }
            }
        }

        if (totalMoney <= 0) {
            MessageUtil.msg(p, "<yellow>No tienes nada que puedas vender.");
            return;
        }

        QSMarketPlus.economy.depositPlayer(p, totalMoney);
        MessageUtil.msg(p, "<green>Vendiste todo tu inventario por <white>$" + totalMoney);
    }

    private ShopItem findShopItem(ItemStack item) {
        for (ShopCategory cat : categoryManager.getCategories().values()) {
            itemManager.loadCategoryItems(cat);
            for (ShopItem si : itemManager.getItems(cat.getId()).values()) {
                if (item.getType() == si.getRealItem().getType()) return si;
            }
        }
        return null;
    }



}
