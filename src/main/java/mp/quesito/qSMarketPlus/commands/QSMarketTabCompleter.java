package mp.quesito.qSMarketPlus.commands;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.shop.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class QSMarketTabCompleter implements TabCompleter {

    private final QSMarketPlus plugin;

    public QSMarketTabCompleter(QSMarketPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 0) return Collections.emptyList();

        List<String> completions = new ArrayList<>();

        switch (args.length) {
            case 1 -> { // /qsmarket <subcomando>
                addIfPerm(completions, sender, "set", "qsmarket.shop.set");
                addIfPerm(completions, sender, "setitem", "qsmarket.signshop.setitem");
                addIfPerm(completions, sender, "additem", "qsmarket.admin.additem");
                addIfPerm(completions, sender, "reload", "qsmarket.admin.reload");
                addIfPerm(completions, sender, "sellstick", "qsmarket.admin.sellstick");
                return filter(completions, args[0]);
            }
            case 2 -> {
                switch (args[0].toLowerCase()) {
                    case "set" -> completions.addAll(List.of("1", "5", "10", "50", "100"));
                    case "setitem", "additem" -> completions.addAll(plugin.getCategoryManager().getCategories().keySet());
                    case "sellstick" -> { // Tab para nombres de jugadores
                        Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
                    }
                }
                return filter(completions, args[1]);
            }
            case 3 -> {
                switch (args[0].toLowerCase()) {
                    case "set" -> completions.addAll(List.of("1", "16", "32", "64"));
                    case "setitem" -> { // Item IDs de la categoría
                        String categoryId = args[1].toLowerCase();
                        plugin.getItemManager().getItems(categoryId).values()
                                .forEach(item -> completions.add(item.getId()));
                    }
                    case "additem" -> completions.addAll(List.of("1", "5", "10", "50", "100")); // Precio compra
                }
                return filter(completions, args[2]);
            }
            case 4 -> { // /qsmarket additem <categoria> <precio_compra> <precio_venta>
                if (args[0].equalsIgnoreCase("additem")) {
                    completions.addAll(List.of("1", "5", "10", "50", "100")); // Precio venta
                    return filter(completions, args[3]);
                }
            }
        }

        return Collections.emptyList();
    }

    // ===================== FILTRAR =====================
    private List<String> filter(List<String> list, String arg) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(arg.toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
    }

    // ===================== HELPER =====================
    private void addIfPerm(List<String> list, CommandSender sender, String subCommand, String perm) {
        if (sender.hasPermission(perm)) list.add(subCommand);
    }
}
