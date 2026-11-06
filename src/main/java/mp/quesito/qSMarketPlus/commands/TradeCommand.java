package mp.quesito.qSMarketPlus.commands;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.trade.TradeHistoryMenu;
import mp.quesito.qSMarketPlus.trade.TradeManager;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TradeCommand implements CommandExecutor {

    private final QSMarketPlus plugin;

    public TradeCommand(QSMarketPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cSolo jugadores pueden usar este comando.");
            return true;
        }

        TradeManager manager = plugin.getTradeManager();

        if (args.length == 0) {
            MessageUtil.lang(p, "trade.usage");
            return true;
        }

        String sub = args[0].toLowerCase();

        // =============================================
        // /trade history [jugador]
        // =============================================
        if (sub.equals("history")) {
            if (args.length >= 2) {
                if (!p.hasPermission("qsmarket.trade.history.others")) {
                    MessageUtil.lang(p, "trade.no-permission");
                    return true;
                }
                UUID target = Bukkit.getOfflinePlayer(args[1]).getUniqueId();
                TradeHistoryMenu.open(p, target, 1);
                return true;
            }

            if (!p.hasPermission("qsmarket.trade.history")) {
                MessageUtil.lang(p, "trade.no-permission");
                return true;
            }
            TradeHistoryMenu.open(p, p.getUniqueId(), 1);
            return true;
        }

        // =============================================
        // /trade accept <jugador>
        // =============================================
        if (sub.equals("accept")) {
            if (!p.hasPermission("qsmarket.trade.request")) {
                MessageUtil.lang(p, "trade.no-permission");
                return true;
            }
            if (args.length < 2) {
                MessageUtil.lang(p, "trade.usage");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null || !canSee(p, target)) {
                MessageUtil.lang(p, "trade.not-found");
                return true;
            }
            if (target.equals(p)) {
                MessageUtil.lang(p, "trade.self");
                return true;
            }
            manager.accept(p, target);
            return true;
        }

        // =============================================
        // /trade deny <jugador>
        // =============================================
        if (sub.equals("deny") || sub.equals("decline")) {
            if (!p.hasPermission("qsmarket.trade.request")) {
                MessageUtil.lang(p, "trade.no-permission");
                return true;
            }
            if (args.length < 2) {
                MessageUtil.lang(p, "trade.usage");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null || !canSee(p, target)) {
                MessageUtil.lang(p, "trade.not-found");
                return true;
            }
            if (target.equals(p)) {
                MessageUtil.lang(p, "trade.self");
                return true;
            }
            manager.deny(p, target);
            return true;
        }

        // =============================================
        // /trade <jugador> -> enviar solicitud
        // =============================================
        if (!p.hasPermission("qsmarket.trade.request")) {
            MessageUtil.lang(p, "trade.no-permission");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !canSee(p, target)) {
            MessageUtil.lang(p, "trade.not-found");
            return true;
        }
        if (target.equals(p)) {
            MessageUtil.lang(p, "trade.self");
            return true;
        }

        manager.request(p, target);
        return true;
    }

    private boolean canSee(Player viewer, Player target) {
        return viewer.canSee(target);
    }
}