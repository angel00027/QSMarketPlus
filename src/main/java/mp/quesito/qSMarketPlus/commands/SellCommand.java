package mp.quesito.qSMarketPlus.commands;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.economia.EconomyProvider;
import mp.quesito.qSMarketPlus.manager.CategoryManager;
import mp.quesito.qSMarketPlus.manager.ItemManager;
import mp.quesito.qSMarketPlus.shop.ShopCategory;
import mp.quesito.qSMarketPlus.shop.ShopItem;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
            case "inv" -> openInvSellMenu(p);
            case "check" -> sellCheck(p, args);
            default -> {
                // Avisa del error y acto seguido le pinta la guía de uso completa
                MessageUtil.lang(p, "sell.invalid_arg");
                MessageUtil.lang(p, "sell.usage");
            }
        }

        return true;
    }

    private void sellHand(Player p) {

        if (!p.hasPermission("qsmarketplus.shop")) {
            p.sendMessage("§cNo tienes permiso para usar este comando.");
            return;
        }

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

        plugin.getStatsManager().recordSell(p, findShopCategory(shopItem), shopItem, amount, totalEarnings);

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

        if (!p.hasPermission("qsmarketplus.shop")) {
            p.sendMessage("§cNo tienes permiso para usar este comando.");
            return;
        }

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

        plugin.getStatsManager().recordSell(p, findShopCategory(targetShopItem), targetShopItem, totalEncontrado, totalEarnings);

        String currencyName = plugin.getConfig().getString("economies." + ecoProvider.getName() + ".display-name", ecoProvider.getName());

        MessageUtil.lang(p, "sell.success_similar",
                Placeholder.parsed("amount", String.valueOf(totalEncontrado)),
                Placeholder.parsed("item", targetShopItem.getName()),
                Placeholder.parsed("earnings", String.format("%.2f", totalEarnings)),
                Placeholder.parsed("currency", currencyName)
        );
    }

    private void sellAll(Player p) {

        if (!p.hasPermission("qsmarketplus.shop")) {
            p.sendMessage("§cNo tienes permiso para usar este comando.");
            return;
        }
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
                plugin.getStatsManager().recordSell(p, findShopCategory(shopItem), shopItem, amount, earnings);
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

        // Seguridad: si ninguna categoría está cargada en memoria aún, cargar.
        boolean anyLoaded = false;
        for (ShopCategory cat : categoryManager.getCategories().values()) {
            if (!itemManager.getItems(cat.getId()).isEmpty()) { anyLoaded = true; break; }
        }
        if (!anyLoaded && !categoryManager.getCategories().isEmpty()) {
            itemManager.reloadAllItems();
        }

        ShopItem firstMatch = null;

        for (ShopCategory cat : categoryManager.getCategories().values()) {
            Map<String, ShopItem> items = itemManager.getItems(cat.getId());
            if (items == null) continue;

            for (ShopItem si : items.values()) {
                if (item.getType() == si.getRealItem().getType()) {
                    // Priorizamos la entrada con precio de venta: si hay una variante
                    // "solo comprar" (sell = 0) definida antes, no debe bloquear la venta
                    if (firstMatch == null) firstMatch = si;
                    if (si.getSell() > 0) return si;
                }
            }
        }
        return firstMatch;
    }

    private ShopCategory findShopCategory(ShopItem item) {
        if (item == null || item.getId() == null) return null;
        for (ShopCategory cat : categoryManager.getCategories().values()) {
            Map<String, ShopItem> items = itemManager.getItems(cat.getId());
            if (items != null && items.containsKey(item.getId())) return cat;
        }
        return null;
    }

    // ==============================================================
    //  /sell inv → Menú de venta (poner ítems y vender al cerrar)
    // ==============================================================
    private void openInvSellMenu(Player p) {

        if (!p.hasPermission("qsmarketplus.shop")) {
            p.sendMessage("§cNo tienes permiso para usar este comando.");
            return;
        }

        mp.quesito.qSMarketPlus.holder.SellInvHolder holder = new mp.quesito.qSMarketPlus.holder.SellInvHolder(p);
        Inventory inv = org.bukkit.Bukkit.createInventory(holder, 54,
                mp.quesito.qSMarketPlus.utils.MenuItems.miniToLegacy("<yellow>« Vende tus ítems »</yellow>"));
        holder.setInventory(inv);

        p.openInventory(inv);

        MessageUtil.lang(p, "sell.inv_info");
    }

    // Llamado al cerrar el menú de /sell inv
    public void sellInvOnClose(Player p, Inventory inv) {
        EconomyProvider ecoProvider = null;
        double totalEarnings = 0;
        int totalSold = 0;
        boolean hadItems = false;

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s == null || s.getType().isAir()) continue;
            hadItems = true;

            ShopItem shopItem = findShopItem(s);

            // Sin precio de venta (o ítem no registrado): se devuelve al jugador
            if (shopItem == null || shopItem.getSell() <= 0) {
                String name = s.hasItemMeta() && s.getItemMeta().hasDisplayName()
                        ? s.getItemMeta().getDisplayName()
                        : s.getType().name().replace("_", " ").toLowerCase();
                HashMap<Integer, ItemStack> leftover = p.getInventory().addItem(s.clone());
                for (ItemStack drop : leftover.values()) {
                    p.getWorld().dropItemNaturally(p.getLocation(), drop);
                }
                MessageUtil.lang(p, "sell.returned_item",
                        Placeholder.parsed("amount", String.valueOf(s.getAmount())),
                        Placeholder.parsed("item", name));
                continue;
            }

            EconomyProvider eco = plugin.getEconomyManager().get(shopItem.getEconomy());
            if (eco == null) {
                HashMap<Integer, ItemStack> leftover = p.getInventory().addItem(s.clone());
                for (ItemStack drop : leftover.values()) {
                    p.getWorld().dropItemNaturally(p.getLocation(), drop);
                }
                continue;
            }
            ecoProvider = eco;

            int amount = s.getAmount();
            double earnings = amount * shopItem.getSell();

            inv.setItem(i, null);
            totalEarnings += earnings;
            totalSold += amount;

            shopItem.executeSellCommands(p);
            plugin.getStatsManager().recordSell(p, findShopCategory(shopItem), shopItem, amount, earnings);
        }

        if (!hadItems) {
            MessageUtil.lang(p, "sell.inv_empty");
            return;
        }

        if (totalSold > 0 && ecoProvider != null) {
            String currencyName = plugin.getConfig().getString("economies." + ecoProvider.getName() + ".display-name", ecoProvider.getName());
            MessageUtil.lang(p, "sell.success_inv",
                    Placeholder.parsed("amount", String.valueOf(totalSold)),
                    Placeholder.parsed("earnings", String.format("%.2f", totalEarnings)),
                    Placeholder.parsed("currency", currencyName)
            );
            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        }
    }

    // ==============================================================
    //  /sell check [material|hand] → Verificar configuración y duplicados
    // ==============================================================
    private void sellCheck(Player p, String[] args) {

        if (!p.hasPermission("qsmarketplus.shop")) {
            p.sendMessage("§cNo tienes permiso para usar este comando.");
            return;
        }

        Material target;
        if (args.length >= 2) {
            target = Material.matchMaterial(args[1]);
            if (target == null) {
                p.sendMessage("§cMaterial inválido: " + args[1] + ". Ej: §f/sell check ender_pearl");
                return;
            }
        } else {
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType().isAir()) {
                MessageUtil.lang(p, "sell.hand_empty");
                return;
            }
            target = hand.getType();
        }

        p.sendMessage("§e=== Verificación de ítem: §f" + target.name() + " §e===");

        int totalEntries = 0;
        Map<String, Integer> perCategory = new LinkedHashMap<>();

        for (ShopCategory cat : categoryManager.getCategories().values()) {
            File file = new File(plugin.getDataFolder(), "items/" + cat.getId() + ".yml");
            if (!file.exists()) continue;

            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection sec = cfg.getConfigurationSection("items");
            if (sec == null) continue;

            int catMatches = 0;

            for (String id : sec.getKeys(false)) {
                Material entryMat = Material.matchMaterial(sec.getString(id + ".material", ""));
                if (entryMat != target) continue;

                catMatches++;
                totalEntries++;

                boolean hasItemstack = isSet(sec.getString(id + ".itemstack"));
                boolean hasTexture = isSet(sec.getString(id + ".texture"));
                boolean hasQst = isSet(sec.getString(id + ".qstexture"));
                boolean simple = !hasItemstack && !hasTexture && !hasQst;

                double buy = sec.getDouble(id + ".buy", 0);
                double sell = sec.getDouble(id + ".sell", 0);
                String eco = sec.getString(id + ".economy", "vault");

                String typeTag = simple ? "§7[simple/material]"
                        : (hasItemstack ? "§d[base64]"
                        : (hasQst ? "§d[custom qstexture]" : "§d[custom texture]"));

                p.sendMessage("§7  " + cat.getId() + " › §f" + id
                        + "  §7buy: " + (buy > 0 ? "§a" + buy : "§8-")
                        + "  §7sell: " + (sell > 0 ? "§a" + sell : "§8-")
                        + "  §7eco: §f" + eco + "  " + typeTag);

                if (simple && sell <= 0) {
                    p.sendMessage("     §c⚠ Entrada simple SIN precio de venta.");
                }
            }

            if (catMatches > 0) perCategory.put(cat.getId(), catMatches);
        }

        if (totalEntries == 0) {
            p.sendMessage("§cNo hay ninguna entrada configurada para este material en ninguna categoría.");
            p.sendMessage("§7Añádela en §fitems/<categoría>.yml§7 con los precios para poder venderlo/comprarlo.");
            return;
        }

        if (perCategory.size() > 1) {
            p.sendMessage("§e⚠ Duplicado entre categorías: está en §f" + String.join("§7, §f", perCategory.keySet()) + "§e.");
            p.sendMessage("§7Al vender se usará la primera entrada con sell > 0 (orden de categorías).");
        }

        for (Map.Entry<String, Integer> en : perCategory.entrySet()) {
            if (en.getValue() > 1) {
                p.sendMessage("§c⚠ " + en.getValue() + " entradas con el mismo material dentro de la categoría §f" + en.getKey() + "§c (sin base64/textura se venderían \"a ciegas\").");
            }
        }

        ShopItem usedForSell = itemManager.findSellable(new ItemStack(target));
        if (usedForSell != null) {
            p.sendMessage("§aAl vender este ítem se usará: §f" + usedForSell.getId()
                    + " §7(sell: " + (usedForSell.getSell() > 0 ? "§a" : "§8") + usedForSell.getSell() + "§7)");
        } else {
            p.sendMessage("§cNo se podría vender este ítem (sin entrada con precio de venta).");
        }
    }

    private boolean isSet(String s) {
        return s != null && !s.isEmpty();
    }
}