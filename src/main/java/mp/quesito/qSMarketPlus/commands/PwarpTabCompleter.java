package mp.quesito.qSMarketPlus.commands;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.pwarp.Pwarp;
import mp.quesito.qSMarketPlus.pwarp.PwarpManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class PwarpTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "create", "delete", "desc", "move", "fav"
    );

    private final PwarpManager manager;

    public PwarpTabCompleter(QSMarketPlus plugin) {
        this.manager = plugin.getPwarpManager();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player p)) return Collections.emptyList();

        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0].toLowerCase(Locale.ROOT), SUBCOMMANDS, new ArrayList<>());
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "delete", "del", "borrar":
                case "desc", "setdesc":
                case "move", "mover": {
                    List<String> names = manager.getByOwner(p.getUniqueId()).stream()
                            .map(Pwarp::getName)
                            .collect(Collectors.toList());
                    return StringUtil.copyPartialMatches(args[1].toLowerCase(Locale.ROOT), names, new ArrayList<>());
                }
                case "fav", "favorito": {
                    List<String> names = manager.getAll().stream()
                            .map(Pwarp::getName)
                            .collect(Collectors.toList());
                    return StringUtil.copyPartialMatches(args[1].toLowerCase(Locale.ROOT), names, new ArrayList<>());
                }
            }
        }

        return Collections.emptyList();
    }
}