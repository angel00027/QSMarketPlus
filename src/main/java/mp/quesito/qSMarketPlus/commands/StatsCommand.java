package mp.quesito.qSMarketPlus.commands;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.stats.StatsMenu;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class StatsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cSolo jugadores en línea.");
            return true;
        }

        if (args.length == 0) {
            if (!p.hasPermission("qsmarket.stats")) {
                MessageUtil.lang(p, "no_permission");
                return true;
            }
            StatsMenu.open(p, p.getUniqueId(), 1);
            return true;
        }

        if (!p.hasPermission("qsmarket.stats.others")) {
            MessageUtil.lang(p, "no_permission");
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target != null) {
            StatsMenu.open(p, target.getUniqueId(), 1);
            return true;
        }

        QSMarketPlus plugin = QSMarketPlus.getInstance();
        final String[] foundUuid = {null};
        plugin.getSqlManager().query("""
                SELECT player_uuid FROM shop_transactions
                WHERE LOWER(player_name) = LOWER(?)
                ORDER BY id DESC LIMIT 1
                """, rs -> {
            try {
                if (rs.next()) foundUuid[0] = rs.getString("player_uuid");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, targetName);

        if (foundUuid[0] == null) {
            MessageUtil.lang(p, "stats_no_data");
            return true;
        }

        try {
            StatsMenu.open(p, UUID.fromString(foundUuid[0]), 1);
        } catch (IllegalArgumentException ex) {
            MessageUtil.lang(p, "stats_no_data");
        }
        return true;
    }
}