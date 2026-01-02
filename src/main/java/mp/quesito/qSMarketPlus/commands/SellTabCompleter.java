package mp.quesito.qSMarketPlus.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class SellTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {

        List<String> list = new ArrayList<>();

        // Solo sugerencias para jugadores
        if (!sender.isOp() && !(sender instanceof org.bukkit.entity.Player)) {
            return list;
        }

        // Primera palabra del comando: /sell <TAB>
        if (args.length == 1) {

            list.add("hand");
            list.add("similar");
            list.add("all");

            return filter(list, args[0]);
        }

        return list;
    }

    private List<String> filter(List<String> options, String input) {
        List<String> result = new ArrayList<>();
        for (String s : options) {
            if (s.toLowerCase().startsWith(input.toLowerCase())) {
                result.add(s);
            }
        }
        return result;
    }
}
