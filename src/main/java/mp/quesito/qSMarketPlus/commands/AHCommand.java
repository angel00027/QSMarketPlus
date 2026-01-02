package mp.quesito.qSMarketPlus.commands;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.auction.menu.AHConfirmMenu;
import mp.quesito.qSMarketPlus.auction.menu.AHMenu;
import mp.quesito.qSMarketPlus.auction.menu.AHHistoryMenu;
import mp.quesito.qSMarketPlus.auction.menu.AHExpiredMenu;
import mp.quesito.qSMarketPlus.utils.Lang;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;

public class AHCommand implements CommandExecutor {

    private final QSMarketPlus plugin;

    public AHCommand(QSMarketPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player p)) {
            sender.sendMessage("Solo jugadores pueden usar esto.");
            return true;
        }

        // /ah → menú principal
        if (args.length == 0) {
            AHMenu.open(p);
            return true;
        }

        String sub = args[0].toLowerCase();

        // ==============================================================
        // /ah history → Historial
        // ==============================================================
        if (sub.equals("history")) {

            if (!p.hasPermission("qsmarket.history")) {
                Lang.msg(p, "no_permission");
                return true;
            }

            AHHistoryMenu.open(p, 1);
            return true;
        }

        // ==============================================================
        // /ah expired → Items expirados
        // ==============================================================
        if (sub.equals("expired")) {

            if (!p.hasPermission("qsmarket.expired")) {
                Lang.msg(p, "no_permission");
                return true;
            }

            AHExpiredMenu.open(p, 1);
            return true;
        }

    // ==============================================================
    // /ah sell <precio>
    // ==============================================================
        if (sub.equals("sell")) {

            if (args.length < 2) {
                Lang.msg(p, "ah_price_error");
                return true;
            }

            double price;
            try {
                price = Double.parseDouble(args[1]);
            } catch (Exception ex) {
                Lang.msg(p, "ah_price_error");
                return true;
            }

            ItemStack hand = p.getInventory().getItemInMainHand();

            if (hand == null || hand.getType().isAir()) {
                Lang.msg(p, "ah_no_item");
                return true;
            }

            // =========================
            //   LIMITE DE SUBASTAS
            // =========================
            int limit = plugin.getAuctionManager().getAuctionLimit(p);
            int current = plugin.getAuctionManager().getActiveAuctions(p);

            if (current >= limit) {
                p.sendMessage("§cHas alcanzado tu límite de subastas activas.");
                p.sendMessage("§7Tu límite actual es: §e" + limit);
                return true;
            }

            int slot = p.getInventory().getHeldItemSlot();
            ItemStack toSell = hand.clone();

            AHConfirmMenu.openSingle(p, toSell, price, slot);
            return true;
        }

        // ==============================================================
        // /ah sellinv <precio>
        // ==============================================================
        if (sub.equals("sellinv")) {

            if (args.length < 2) {
                Lang.msg(p, "ah_price_error");
                return true;
            }

            double price;
            try {
                price = Double.parseDouble(args[1]);
            } catch (Exception ex) {
                Lang.msg(p, "ah_price_error");
                return true;
            }

            ItemStack[] items = Arrays.stream(p.getInventory().getStorageContents())
                    .filter(i -> i != null && !i.getType().isAir())
                    .map(ItemStack::clone)
                    .toArray(ItemStack[]::new);

            if (items.length == 0) {
                Lang.msg(p, "ah_no_item");
                return true;
            }

            // =========================
            //   LIMITE DE SUBASTAS
            // =========================
            int limit = plugin.getAuctionManager().getAuctionLimit(p);
            int current = plugin.getAuctionManager().getActiveAuctions(p);

            if (current >= limit) {
                p.sendMessage("§cHas alcanzado tu límite de subastas activas.");
                p.sendMessage("§7Tu límite actual es: §e" + limit);
                return true;
            }

            AHConfirmMenu.openBulk(p, items, price);
            return true;
        }


        // ==============================================================
        // AYUDA
        // ==============================================================
        p.sendMessage("§eUso:");
        p.sendMessage("§f/ah §7- Abrir casa de subastas");
        p.sendMessage("§f/ah sell <precio> §7- Subastar item en mano");
        p.sendMessage("§f/ah sellinv <precio> §7- Subastar todo el inventario");
        p.sendMessage("§f/ah history §7- Ver tu historial de subastas");
        p.sendMessage("§f/ah expired §7- Ver tus subastas expiradas");

        return true;
    }
}
