package mp.quesito.qSMarketPlus.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlayerShopTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // Argumento 1: <precio>
        if (args.length == 1) {
            List<String> precioSugerencias = Arrays.asList("10", "50", "100", "500");
            StringUtil.copyPartialMatches(args[0], precioSugerencias, completions);
            Collections.sort(completions);
            return completions;
        }

        // Argumento 2: <cantidad>
        if (args.length == 2) {
            List<String> cantidadSugerencias = Arrays.asList("1", "16", "32", "64");
            StringUtil.copyPartialMatches(args[1], cantidadSugerencias, completions);
            Collections.sort(completions);
            return completions;
        }

        return Collections.emptyList();
    }
}