package mp.quesito.qSMarketPlus.commands;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.shop.ShopItem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;
import java.util.stream.Collectors;

public class QSMarketTabCompleter implements TabCompleter {

    private final QSMarketPlus plugin;

    public QSMarketTabCompleter(QSMarketPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        List<String> completions = new ArrayList<>();

        /* =====================================================
         * /qsmarket <subcomando>
         * ===================================================== */
        if (args.length == 1) {

            if (sender.hasPermission("qsmarket.shop.set"))
                completions.add("set");

            if (sender.hasPermission("qsmarket.signshop.setitem"))
                completions.add("setitem");

            if (sender.hasPermission("qsmarket.admin.additem"))
                completions.add("additem");

            if (sender.hasPermission("qsmarket.admin.reload"))
                completions.add("reload");

            return filter(completions, args[0]);
        }

        /* =====================================================
         * /qsmarket set <precio>
         * ===================================================== */
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return filter(
                    List.of("1", "5", "10", "50", "100"),
                    args[1]
            );
        }

        /* =====================================================
         * /qsmarket set <precio> <cantidad>
         * ===================================================== */
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return filter(
                    List.of("1", "16", "32", "64"),
                    args[2]
            );
        }

        /* =====================================================
         * /qsmarket setitem <categoria>
         * ===================================================== */
        if (args.length == 2 && args[0].equalsIgnoreCase("setitem")) {
            completions.addAll(
                    plugin.getCategoryManager().getCategories().keySet()
            );
            return filter(completions, args[1]);
        }

        /* =====================================================
         * /qsmarket setitem <categoria> <itemId>
         * ===================================================== */
        if (args.length == 3 && args[0].equalsIgnoreCase("setitem")) {

            String categoryId = args[1].toLowerCase();

            plugin.getItemManager().getItems(categoryId).values()
                    .forEach(item -> completions.add(item.getId()));

            return filter(completions, args[2]);
        }

        /* =====================================================
         * /qsmarket additem <categoria>
         * ===================================================== */
        if (args.length == 2 && args[0].equalsIgnoreCase("additem")) {
            completions.addAll(
                    plugin.getCategoryManager().getCategories().keySet()
            );
            return filter(completions, args[1]);
        }

        /* =====================================================
         * /qsmarket additem <categoria> <precio_compra>
         * ===================================================== */
        if (args.length == 3 && args[0].equalsIgnoreCase("additem")) {
            return filter(
                    List.of("1", "5", "10", "50", "100"),
                    args[2]
            );
        }

        /* =====================================================
         * /qsmarket additem <categoria> <precio_compra> <precio_venta>
         * ===================================================== */
        if (args.length == 4 && args[0].equalsIgnoreCase("additem")) {
            return filter(
                    List.of("1", "5", "10", "50", "100"),
                    args[3]
            );
        }

        return completions;
    }

    private List<String> filter(List<String> list, String arg) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(arg.toLowerCase()))
                .collect(Collectors.toList());
    }
}
