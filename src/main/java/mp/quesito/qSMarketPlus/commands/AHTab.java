package mp.quesito.qSMarketPlus.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AHTab implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {

        if (!(sender instanceof Player p)) return null;

        List<String> list = new ArrayList<>();

        // =======================================================
        // /ah <primer argumento>
        // =======================================================
        if (args.length == 1) {

            if (p.hasPermission("qsmarket.history"))
                list.add("history");

            if (p.hasPermission("qsmarket.expired"))
                list.add("expired");

            list.add("sell");
            list.add("sellinv");

            return filter(list, args[0]);
        }

        // =======================================================
        // /ah sell <precio>
        // =======================================================
        if (args.length == 2 && args[0].equalsIgnoreCase("sell")) {

            list.add("<precio>");
            return filter(list, args[1]);
        }

        // =======================================================
        // /ah sellinv <precio>
        // =======================================================
        if (args.length == 2 && args[0].equalsIgnoreCase("sellinv")) {

            list.add("<precio>");
            return filter(list, args[1]);
        }

        return null;
    }

    // Filtro básico
    private List<String> filter(List<String> base, String arg) {
        List<String> result = new ArrayList<>();
        for (String s : base) {
            if (s.toLowerCase().startsWith(arg.toLowerCase()))
                result.add(s);
        }
        return result;
    }
}
