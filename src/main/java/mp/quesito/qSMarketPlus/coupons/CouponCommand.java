package mp.quesito.qSMarketPlus.coupons;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.coupons.CouponManager.Coupon;
import mp.quesito.qSMarketPlus.coupons.CouponManager.Template;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * /cupon                                  abre "Mis cupones"
 * /cupon canjear <codigo>                 canjea un codigo
 * /cupon generar <plantilla> [jugador] [cantidad]   (admin) crea codigos
 * /cupon borrar <codigo>                  (admin) elimina un codigo
 * /cupon plantillas                       (admin) lista las plantillas
 */
public class CouponCommand implements CommandExecutor, TabCompleter {

    private static final String ADMIN = "qsmarket.cupones.admin";

    private CouponManager manager() {
        return QSMarketPlus.getInstance().getCouponManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            if (sender instanceof Player p) CouponMenu.open(p, 1);
            else help(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {

            case "canjear", "redeem" -> {
                if (!(sender instanceof Player p)) return true;
                if (args.length < 2) {
                    manager().send(p, "uso-canjear", "<red>Uso: /cupon canjear <código>");
                    return true;
                }
                CouponListener.redeem(p, args[1]);
            }

            case "generar", "dar" -> {
                if (!sender.hasPermission(ADMIN)) return noPerm(sender);
                generate(sender, args);
            }

            case "borrar" -> {
                if (!sender.hasPermission(ADMIN)) return noPerm(sender);
                if (args.length < 2) {
                    sender.sendMessage("§cUso: /cupon borrar <código>");
                    return true;
                }
                sender.sendMessage(manager().delete(args[1]) ? "§aCupón eliminado." : "§cNo existe ese código.");
            }

            case "plantillas" -> {
                if (!sender.hasPermission(ADMIN)) return noPerm(sender);
                for (Template t : manager().templates()) {
                    sender.sendMessage(MessageUtil.toLegacy("<yellow>" + t.id() + " <gray>» " + t.name()
                            + " <gray>(" + CouponManager.money(t.percent()) + "% en " + manager().categoryNames(t)
                            + ", " + (t.uses() == CouponManager.UNLIMITED ? "usos ilimitados" : t.uses() + " uso(s)") + ")"));
                }
            }

            default -> help(sender);
        }
        return true;
    }

    private void generate(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sender.sendMessage("§cUso: /cupon generar <plantilla> [jugador] [cantidad]");
            return;
        }

        Template template = manager().template(args[1]);
        if (template == null) {
            sender.sendMessage("§cNo existe la plantilla '" + args[1] + "'. Mira /cupon plantillas");
            return;
        }

        OfflinePlayer target = null;
        if (args.length >= 3 && !args[2].equals("-")) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                // Solo jugadores que ya entraron alguna vez
                for (OfflinePlayer known : Bukkit.getOfflinePlayers()) {
                    if (known.getName() != null && known.getName().equalsIgnoreCase(args[2])) {
                        target = known;
                        break;
                    }
                }
            }
            if (target == null) {
                sender.sendMessage("§cEse jugador nunca entró al servidor.");
                return;
            }
        }

        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Math.min(50, Integer.parseInt(args[3])));
            } catch (NumberFormatException ex) {
                sender.sendMessage("§cLa cantidad debe ser un número.");
                return;
            }
        }

        java.util.UUID creator = sender instanceof Player p ? p.getUniqueId() : null;
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            Coupon c = manager().generate(template, target == null ? null : target.getUniqueId(), creator);
            codes.add(c.code());
        }

        if (target != null) {
            sender.sendMessage(MessageUtil.toLegacy("<green>✔ " + amount + " cupón(es) <white>" + template.id()
                    + "</white> para <white>" + target.getName()));
            if (target.getPlayer() != null) {
                manager().send(target.getPlayer(), "recibido",
                        "<green>¡Recibiste un cupón! <white><name></white> <gray>· míralo en /cupon",
                        "name", template.name());
            }
        } else {
            sender.sendMessage(MessageUtil.toLegacy("<green>✔ Códigos de <white>" + template.id() + "</white> (clic para copiar):"));
        }

        // Los codigos sueltos se muestran para poder entregarlos; clic = copiar
        if (target == null) {
            for (String code : codes) {
                if (sender instanceof Player p) {
                    TextComponent line = new TextComponent("  " + code);
                    line.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
                    line.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, code));
                    line.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Clic para copiar")));
                    p.spigot().sendMessage(line);
                } else {
                    sender.sendMessage("  " + code);
                }
            }
        }
    }

    private boolean noPerm(CommandSender sender) {
        sender.sendMessage("§cNo tienes permiso.");
        return true;
    }

    private void help(CommandSender sender) {
        sender.sendMessage(MessageUtil.toLegacy("<gold>Cupones de la tienda"));
        sender.sendMessage(MessageUtil.toLegacy("<yellow>/cupon <gray>- tus cupones"));
        sender.sendMessage(MessageUtil.toLegacy("<yellow>/cupon canjear <código> <gray>- canjear un código"));
        if (sender.hasPermission(ADMIN)) {
            sender.sendMessage(MessageUtil.toLegacy("<yellow>/cupon generar <plantilla> [jugador|-] [cantidad]"));
            sender.sendMessage(MessageUtil.toLegacy("<yellow>/cupon borrar <código> <gray>· <yellow>/cupon plantillas"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        boolean admin = sender.hasPermission(ADMIN);
        Stream<String> options = Stream.empty();

        if (args.length == 1) {
            options = admin ? Stream.of("canjear", "generar", "borrar", "plantillas") : Stream.of("canjear");
        } else if (admin && args[0].equalsIgnoreCase("generar")) {
            if (args.length == 2) options = manager().templates().stream().map(Template::id);
            else if (args.length == 3) options = Stream.concat(Stream.of("-"),
                    Bukkit.getOnlinePlayers().stream().map(Player::getName));
            else if (args.length == 4) options = Stream.of("1", "5", "10");
        }

        if (args.length == 0) return Collections.emptyList();
        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.filter(o -> o.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
    }
}
