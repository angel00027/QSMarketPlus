package mp.quesito.qSMarketPlus.commands;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.economia.EconomyProvider;
import mp.quesito.qSMarketPlus.manager.CategoryManager;
import mp.quesito.qSMarketPlus.manager.ItemManager;
import mp.quesito.qSMarketPlus.shop.ShopCategory;
import mp.quesito.qSMarketPlus.shop.ShopItem;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

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
            sender.sendMessage("§cSolo jugadores en línea.");
            return true;
        }

        // Si no pone argumentos, muestra la guía detallada
        if (args.length == 0) {
            MessageUtil.lang(p, "sell.usage");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "hand" -> sellHand(p);
            case "similar" -> sellSimilar(p);
            case "all" -> sellAll(p);
            default -> {
                // Avisa del error y acto seguido le pinta la guía de uso completa
                MessageUtil.lang(p, "sell.invalid_arg");
                MessageUtil.lang(p, "sell.usage");
            }
        }

        return true;
    }

    private void sellHand(Player p) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            MessageUtil.lang(p, "sell.hand_empty");
            return;
        }

        ShopItem shopItem = findShopItem(hand);
        if (shopItem == null || shopItem.getSell() <= 0) {
            MessageUtil.lang(p, "sell.no_sell_price");
            return;
        }

        EconomyProvider ecoProvider = plugin.getEconomyManager().get(shopItem.getEconomy());
        if (ecoProvider == null) {
            p.sendMessage("§cError interno: El sistema de economía '" + shopItem.getEconomy() + "' no está disponible.");
            return;
        }

        int amount = hand.getAmount();
        double totalEarnings = shopItem.getSell() * amount;

        p.getInventory().setItemInMainHand(null);

        ecoProvider.deposit(p, totalEarnings);
        shopItem.executeSellCommands(p);

        String currencyName = plugin.getConfig().getString("economies." + ecoProvider.getName() + ".display-name", ecoProvider.getName());

        // Enviamos el mensaje procesando dinámicamente los tags de MiniMessage
        MessageUtil.lang(p, "sell.success_hand",
                Placeholder.parsed("amount", String.valueOf(amount)),
                Placeholder.parsed("item", shopItem.getName()),
                Placeholder.parsed("earnings", String.format("%.2f", totalEarnings)),
                Placeholder.parsed("currency", currencyName)
        );
    }

    private void sellSimilar(Player p) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            MessageUtil.lang(p, "sell.hand_empty");
            return;
        }

        ShopItem targetShopItem = findShopItem(hand);
        if (targetShopItem == null || targetShopItem.getSell() <= 0) {
            MessageUtil.lang(p, "sell.no_sell_price");
            return;
        }

        EconomyProvider ecoProvider = plugin.getEconomyManager().get(targetShopItem.getEconomy());
        if (ecoProvider == null) {
            p.sendMessage("§cError interno: El sistema de economía '" + targetShopItem.getEconomy() + "' no está disponible.");
            return;
        }

        int totalEncontrado = 0;
        PlayerInventory inv = p.getInventory();

        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            if (s == null || s.getType().isAir()) continue;

            ShopItem currentShopItem = findShopItem(s);
            if (currentShopItem != null && currentShopItem.getId().equals(targetShopItem.getId())) {
                totalEncontrado += s.getAmount();
                inv.setItem(i, null);
            }
        }

        if (totalEncontrado <= 0) {
            MessageUtil.lang(p, "sell.no_items_similar");
            return;
        }

        double totalEarnings = totalEncontrado * targetShopItem.getSell();

        ecoProvider.deposit(p, totalEarnings);
        targetShopItem.executeSellCommands(p);

        String currencyName = plugin.getConfig().getString("economies." + ecoProvider.getName() + ".display-name", ecoProvider.getName());

        MessageUtil.lang(p, "sell.success_similar",
                Placeholder.parsed("amount", String.valueOf(totalEncontrado)),
                Placeholder.parsed("item", targetShopItem.getName()),
                Placeholder.parsed("earnings", String.format("%.2f", totalEarnings)),
                Placeholder.parsed("currency", currencyName)
        );
    }

    private void sellAll(Player p) {
        PlayerInventory inv = p.getInventory();
        boolean algunItemVendido = false;

        for (ShopCategory cat : categoryManager.getCategories().values()) {
            itemManager.loadCategoryItems(cat);
        }

        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            if (s == null || s.getType().isAir()) continue;

            ShopItem shopItem = findShopItem(s);
            if (shopItem != null && shopItem.getSell() > 0) {
                EconomyProvider ecoProvider = plugin.getEconomyManager().get(shopItem.getEconomy());
                if (ecoProvider == null) continue;

                int amount = s.getAmount();
                double earnings = amount * shopItem.getSell();

                inv.setItem(i, null);

                ecoProvider.deposit(p, earnings);
                shopItem.executeSellCommands(p);
                algunItemVendido = true;

                String currencyName = plugin.getConfig().getString("economies." + ecoProvider.getName() + ".display-name", ecoProvider.getName());

                MessageUtil.lang(p, "sell.success_all_item",
                        Placeholder.parsed("amount", String.valueOf(amount)),
                        Placeholder.parsed("item", shopItem.getName()),
                        Placeholder.parsed("earnings", String.format("%.2f", earnings)),
                        Placeholder.parsed("currency", currencyName)
                );
            }
        }

        if (!algunItemVendido) {
            MessageUtil.lang(p, "sell.no_items_all");
            return;
        }

        MessageUtil.lang(p, "sell.success_all_done");
    }

    private ShopItem findShopItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;

        for (ShopCategory cat : categoryManager.getCategories().values()) {
            Map<String, ShopItem> items = itemManager.getItems(cat.getId());
            if (items == null) continue;

            for (ShopItem si : items.values()) {
                if (item.getType() == si.getRealItem().getType()) {
                    return si;
                }
            }
        }
        return null;
    }
}