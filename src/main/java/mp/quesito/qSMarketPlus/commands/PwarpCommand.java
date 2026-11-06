package mp.quesito.qSMarketPlus.commands;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.pwarp.Pwarp;
import mp.quesito.qSMarketPlus.pwarp.PwarpManager;
import mp.quesito.qSMarketPlus.pwarp.menu.PwarpMenu;
import mp.quesito.qSMarketPlus.utils.Lang;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

public class PwarpCommand implements CommandExecutor {

    private static final int MAX_DESC = 40;

    private final PwarpManager manager;

    public PwarpCommand(QSMarketPlus plugin) {
        this.manager = plugin.getPwarpManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Solo jugadores pueden usar este comando.");
            return true;
        }

        if (args.length == 0) {
            PwarpMenu.open(p);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create", "crear", "set" -> create(p, args);
            case "delete", "del", "borrar", "remove" -> delete(p, args);
            case "desc", "descripcion", "setdesc", "description" -> description(p, args);
            case "move", "mover", "setloc" -> move(p, args);
            case "fav", "favorite", "favorito" -> favorite(p, args);
            default -> Lang.msg(p, "pwarp.usage");
        }
        return true;
    }

    private void create(Player p, String[] args) {
        if (!p.hasPermission("qsmarket.pwarp.create")) {
            Lang.msg(p, "pwarp.no-permission");
            return;
        }
        if (args.length < 2) {
            Lang.msg(p, "pwarp.usage-create");
            return;
        }

        String display = args[1];
        String lower = display.toLowerCase(Locale.ROOT);

        if (!lower.matches("[a-z0-9_]{3,20}")) {
            Lang.msg(p, "pwarp.invalid-name");
            return;
        }
        if (manager.get(lower) != null) {
            Lang.msg(p, "pwarp.name-taken", "warp", display);
            return;
        }
        if (manager.getWarpCount(p.getUniqueId()) >= manager.getMaxWarps(p)) {
            Lang.msg(p, "pwarp.limit-reached", "limit", manager.getMaxWarps(p));
            return;
        }

        manager.createPwarp(lower, p.getUniqueId(), p.getName(), p.getLocation());
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.4f);
        Lang.msg(p, "pwarp.created", "warp", display);
    }

    private void delete(Player p, String[] args) {
        if (!p.hasPermission("qsmarket.pwarp.delete")) {
            Lang.msg(p, "pwarp.no-permission");
            return;
        }
        if (args.length < 2) {
            Lang.msg(p, "pwarp.usage-delete");
            return;
        }

        Pwarp warp = manager.get(args[1]);
        if (warp == null) {
            Lang.msg(p, "pwarp.not-found", "warp", args[1]);
            return;
        }
        if (!warp.getOwner().equals(p.getUniqueId())) {
            Lang.msg(p, "pwarp.not-owner");
            return;
        }

        manager.deletePwarp(warp.getName());
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.8f);
        Lang.msg(p, "pwarp.deleted", "warp", warp.getDisplayName());
    }

    private void description(Player p, String[] args) {
        if (!p.hasPermission("qsmarket.pwarp.setdesc")) {
            Lang.msg(p, "pwarp.no-permission");
            return;
        }
        if (args.length < 3) {
            Lang.msg(p, "pwarp.usage-desc");
            return;
        }

        Pwarp warp = manager.get(args[1]);
        if (warp == null) {
            Lang.msg(p, "pwarp.not-found", "warp", args[1]);
            return;
        }
        if (!warp.getOwner().equals(p.getUniqueId())) {
            Lang.msg(p, "pwarp.not-owner");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(args[i]);
        }
        String desc = MessageUtil.stripLegacy(sb.toString()).trim();
        if (desc.isEmpty()) {
            Lang.msg(p, "pwarp.usage-desc");
            return;
        }
        if (desc.length() > MAX_DESC) {
            Lang.msg(p, "pwarp.desc-too-long");
            return;
        }

        manager.updateDescription(warp, desc);
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.3f);
        Lang.msg(p, "pwarp.desc-set", "warp", warp.getDisplayName());
    }

    private void move(Player p, String[] args) {
            if (!p.hasPermission("qsmarket.pwarp.setloc")) {
            Lang.msg(p, "pwarp.no-permission");
            return;
        }
        if (args.length < 2) {
            Lang.msg(p, "pwarp.usage-move");
            return;
        }

        Pwarp warp = manager.get(args[1]);
        if (warp == null) {
            Lang.msg(p, "pwarp.not-found", "warp", args[1]);
            return;
        }
        if (!warp.getOwner().equals(p.getUniqueId())) {
            Lang.msg(p, "pwarp.not-owner");
            return;
        }

        manager.updateLocation(warp, p.getLocation());
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.3f);
        Lang.msg(p, "pwarp.moved", "warp", warp.getDisplayName());
    }

    private void favorite(Player p, String[] args) {
        if (!p.hasPermission("qsmarket.pwarp.favorite")) {
            Lang.msg(p, "pwarp.no-permission");
            return;
        }
        if (args.length < 2) {
            Lang.msg(p, "pwarp.usage-fav");
            return;
        }

        Pwarp warp = manager.get(args[1]);
        if (warp == null) {
            Lang.msg(p, "pwarp.not-found", "warp", args[1]);
            return;
        }

        boolean added = manager.toggleFavorite(p.getUniqueId(), warp.getName());
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        Lang.msg(p, added ? "pwarp.fav-added" : "pwarp.fav-removed", "warp", warp.getDisplayName());
    }
}