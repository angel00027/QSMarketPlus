package mp.quesito.qSMarketPlus.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TradeTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {

        List<String> list = new ArrayList<>();

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();

            if ("accept".startsWith(prefix)) list.add("accept");
            if ("deny".startsWith(prefix)) list.add("deny");
            if ("history".startsWith(prefix)) list.add("history");

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(sender) && p.getName().toLowerCase().startsWith(prefix)) {
                    list.add(p.getName());
                }
            }

        } else if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(sender) && p.getName().toLowerCase().startsWith(prefix)) {
                    list.add(p.getName());
                }
            }
        }

        return list;
    }
}