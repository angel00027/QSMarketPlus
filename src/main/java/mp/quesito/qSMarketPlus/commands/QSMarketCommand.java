package mp.quesito.qSMarketPlus.commands;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.database.SQLManager;
import mp.quesito.qSMarketPlus.manager.*;
import mp.quesito.qSMarketPlus.shop.*;
import mp.quesito.qSMarketPlus.utils.ItemSerializer;
import mp.quesito.qSMarketPlus.utils.Lang;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class QSMarketCommand implements CommandExecutor {

    private final QSMarketPlus plugin;
    private final PlayerShopManager playerShopManager;
    private final SignShopManager signShopManager;
    private final ItemManager itemManager;

    public QSMarketCommand(QSMarketPlus plugin) {
        this.plugin = plugin;
        this.playerShopManager = plugin.getShopManager();
        this.signShopManager = plugin.getSignShopManager();
        this.itemManager = plugin.getItemManager();
    }



    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        /* ======================================================
         * /qsmarket reload
         * ====================================================== */
        if (args[0].equalsIgnoreCase("reload")) {

            plugin.reloadConfig();

            plugin.getCategoryManager().reload();
            plugin.getItemManager().reloadAllItems();
            plugin.getItemManager().reloadItemsMenuConfig();

            plugin.actionMenuConfig.reload();
            plugin.confirmMenuConfig.reload();
            plugin.amountMenuConfig.reload();

            AHConfig.reload(plugin);
            Lang.init(plugin);

            sender.sendMessage(ChatColor.GREEN + "✔ Configuraciones recargadas.");
            return true;
        }

        /* ======================================================
         * /qsmarket set <precio> <cantidad> (PlayerShop)
         * ====================================================== */

        /*
        if (args[0].equalsIgnoreCase("set")) {

            if (!hasPerm(sender, "qsmarket.shop.set")) return true;

            if (!(sender instanceof Player p)) {
                sender.sendMessage("§cSolo jugadores.");
                return true;
            }



            if (args.length != 3) {
                p.sendMessage(ChatColor.RED + "Uso: /qsmarket set <precio> <cantidad>");
                return true;
            }

            PlayerShop shop = getPlayerShop(p);
            if (shop == null) {
                p.sendMessage(ChatColor.RED + "No tienes ninguna tienda.");
                return true;
            }

            try {
                double price = Double.parseDouble(args[1]);
                int amount = Integer.parseInt(args[2]);

                shop.setPrice(price);
                shop.setAmountPerSale(amount);
                playerShopManager.updateShop(shop);
                shop.updateSign();

                p.sendMessage(ChatColor.GREEN + "✔ PlayerShop actualizada correctamente.");
            } catch (NumberFormatException e) {
                p.sendMessage(ChatColor.RED + "Valores inválidos.");
            }
            return true;
        }
        */
        /* ======================================================
         * /qsmarket setitem <categoria> <itemId> (SignShop)
         * ====================================================== */

        if (args[0].equalsIgnoreCase("setitem")) {

            if (!hasPerm(sender, "qsmarket.signshop.setitem")) return true;

            if (!(sender instanceof Player p)) {
                sender.sendMessage("§cSolo jugadores.");
                return true;
            }

            if (args.length < 3) {
                p.sendMessage(ChatColor.RED + "Uso: /qsmarket setitem <categoria> <itemId> [economia]");
                return true;
            }

            Block target = p.getTargetBlockExact(5);

            if (target == null || !(target.getState() instanceof Sign sign)) {
                p.sendMessage(ChatColor.RED + "Debes mirar un cartel válido.");
                return true;
            }

            String categoryId = args[1].toLowerCase();
            String itemId = args[2].toLowerCase();

            // Economía opcional
            String economy = args.length >= 4 ? args[3].toLowerCase() : "vault";

            // Verificar que la economía exista
            if (plugin.getEconomyManager().get(economy) == null) {
                p.sendMessage(ChatColor.RED + "La economía '" + economy + "' no está registrada.");
                return true;
            }

            ShopItem item = itemManager.getItem(categoryId, itemId);

            // Si no existe en config, usar el item de la mano
            if (item == null) {

                ItemStack hand = p.getInventory().getItemInMainHand();

                if (hand == null || hand.getType().isAir()) {
                    p.sendMessage(ChatColor.RED + "El item no existe y no tienes nada en la mano.");
                    return true;
                }

                item = new ShopItem(
                        itemId,
                        hand.hasItemMeta() && hand.getItemMeta().hasDisplayName()
                                ? hand.getItemMeta().getDisplayName()
                                : hand.getType().name(),
                        1.0,
                        1.0,
                        economy,
                        ItemSerializer.toBase64(hand)
                );
            }

            SignShop shop = new SignShop(
                    target.getLocation(),
                    item,
                    item.getBuy(),
                    item.getSell(),
                    1,
                    p.getUniqueId().toString()
            );

            signShopManager.addShop(shop, p.getUniqueId().toString());

            shop.updateSign();

            p.sendMessage(ChatColor.GREEN + "✔ SignShop creada correctamente para: " + item.getName() +
                    ChatColor.GRAY + " (" + economy + ")");

            return true;
        }


        /* ======================================================
         * /qsmarket sellstick [jugador]
         * ====================================================== */
        if (args[0].equalsIgnoreCase("sellstick")) {
            if (!hasPerm(sender, "qsmarket.admin.sellstick")) return true;

            Player target = null;

            if (args.length == 2) { // Se especifica jugador
                target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("§cJugador no encontrado.");
                    return true;
                }
            } else if (sender instanceof Player p) { // Sin args, se da a quien ejecuta
                target = p;
            } else {
                sender.sendMessage("§cDebes especificar un jugador válido.");
                return true;
            }

            SellStickItem.give(target);
            sender.sendMessage("§a✔ Sell Stick entregada a " + target.getName());
            return true;
        }

        /* ======================================================
         * /qsmarket resetunique <jugador> [itemId]
         * ====================================================== */
        if (args[0].equalsIgnoreCase("resetunique")) {

            if (!hasPerm(sender, "qsmarket.admin.resetunique")) return true;

            if (args.length < 2) {
                sender.sendMessage("§cUso: /qsmarket resetunique <jugador> [itemId]");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("§cJugador no encontrado.");
                return true;
            }

            String itemId = args.length >= 3 ? args[2].toLowerCase() : null;
            SQLManager sql = plugin.getSqlManager();

            if (itemId == null) {
                // Eliminar todas las compras únicas del jugador
                int removed = sql.update("DELETE FROM unique_purchases WHERE player_uuid = ?", target.getUniqueId().toString());
                sender.sendMessage("§a✔ Se han eliminado " + removed + " compras únicas de " + target.getName() + ".");
            } else {
                // Eliminar un ítem específico
                int removed = sql.update("DELETE FROM unique_purchases WHERE player_uuid = ? AND item_id = ?",
                        target.getUniqueId().toString(), itemId);
                if (removed > 0) {
                    sender.sendMessage("§a✔ Se ha eliminado la compra única del ítem §f" + itemId + "§a para " + target.getName() + ".");
                } else {
                    sender.sendMessage("§cNo se encontró compra única del ítem §f" + itemId + "§c para " + target.getName() + ".");
                }
            }

            return true;
        }

        /* ======================================================
         * /qsmarket additem <categoria> <precio_compra> <precio_venta>
         * ====================================================== */
        if (args[0].equalsIgnoreCase("additem")) {

            if (!hasPerm(sender, "qsmarket.admin.additem")) return true;
            if (!(sender instanceof Player p)) {
                sender.sendMessage("Solo jugadores.");
                return true;
            }

            if (args.length < 4) {
                MessageUtil.msg(p, "<yellow>Uso:</yellow> /qsmarket additem <categoria> <precio_compra> <precio_venta>");
                return true;
            }

            String categoryId = args[1].toLowerCase();
            ShopCategory category = plugin.getCategoryManager().getCategory(categoryId);

            if (category == null) {
                MessageUtil.msg(p, "<red>La categoría <white>" + categoryId + "</white> no existe.");
                return true;
            }

            double buy, sell;
            try {
                buy = Double.parseDouble(args[2]);
                sell = Double.parseDouble(args[3]);
            } catch (NumberFormatException e) {
                MessageUtil.msg(p, "<red>Los precios deben ser números.");
                return true;
            }

            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType().isAir()) {
                MessageUtil.msg(p, "<red>No tienes un ítem en la mano.");
                return true;
            }

            String base64 = ItemSerializer.toBase64(hand);

            MiniMessage mm = MiniMessage.miniMessage();
            LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();

            /* ===================== NAME (LEGACY → MINIMESSAGE) ===================== */
            String itemName;
            if (hand.hasItemMeta() && hand.getItemMeta().hasDisplayName()) {
                itemName = mm.serialize(
                        legacy.deserialize(hand.getItemMeta().getDisplayName())
                );
            } else {
                itemName = "<white>" + hand.getType().name().toLowerCase() + "</white>";
            }

            File file = new File(plugin.getDataFolder(), "items/" + categoryId + ".yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            String id = "item_" + System.currentTimeMillis();

            config.set("items." + id + ".name", itemName);
            config.set("items." + id + ".material", hand.getType().name());
            config.set("items." + id + ".buy", buy);
            config.set("items." + id + ".sell", sell);
            config.set("items." + id + ".itemstack", base64);

            /* ===================== LORE (LEGACY → MINIMESSAGE) ===================== */
            List<String> lore = new ArrayList<>();
            if (hand.hasItemMeta() && hand.getItemMeta().hasLore()) {
                for (String line : hand.getItemMeta().getLore()) {
                    lore.add(
                            mm.serialize(
                                    legacy.deserialize(line)
                            )
                    );
                }
            }
            config.set("items." + id + ".lore", lore);

            /* ===================== SAVE ===================== */
            try {
                config.save(file);
            } catch (IOException e) {
                MessageUtil.msg(p, "<red>Error al guardar el archivo.");
                return true;
            }

            itemManager.loadCategoryItems(category);
            MessageUtil.msg(p, "<green>Ítem añadido correctamente a <yellow>" + categoryId + "</yellow>.");
            return true;


        }

        sendHelp(sender);
        return true;
    }

    /* ===================== HELP ===================== */

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§eQSMarketPlus:");
        sender.sendMessage("§f/qsmarket reload");
        sender.sendMessage("§f/qsmarket set <precio> <cantidad>");
        sender.sendMessage("§f/qsmarket setitem <categoria> <itemId>");
        sender.sendMessage("§f/qsmarket additem <categoria> <precio_compra> <precio_venta>");
        sender.sendMessage("§cUso: /qsmarket resetunique <jugador> [itemId]");


    }

    private boolean hasPerm(CommandSender sender, String perm) {
        if (sender.hasPermission(perm)) return true;
        sender.sendMessage("§cNo tienes permiso para usar este comando.");
        return false;
    }


    /* ===================== UTILS ===================== */

    private PlayerShop getPlayerShop(Player p) {
        List<PlayerShop> shops = playerShopManager.getShops(p.getUniqueId());
        return shops.isEmpty() ? null : shops.get(0);
    }
}
