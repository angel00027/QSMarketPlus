package mp.quesito.qSMarketPlus.commands;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.auction.AuctionItem;
import mp.quesito.qSMarketPlus.auction.menu.AHConfirmMenu;
import mp.quesito.qSMarketPlus.auction.menu.AHMenu;
import mp.quesito.qSMarketPlus.auction.menu.AHHistoryMenu;
import mp.quesito.qSMarketPlus.auction.menu.AHExpiredMenu;
import mp.quesito.qSMarketPlus.database.SQLManager;
import mp.quesito.qSMarketPlus.utils.Lang;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class AHCommand implements CommandExecutor {

    private final QSMarketPlus plugin;

    public AHCommand(QSMarketPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player p)) {
            sender.sendMessage("Solo jugadores pueden usar esto.");
            return true;
        }

        // /ah → menú principal
        if (args.length == 0) {
            AHMenu.open(p);
            return true;
        }

        String sub = args[0].toLowerCase();

        // ==============================================================
        // /ah history [jugador] → Historial (admin puede ver otros)
        // ==============================================================
        if (sub.equals("history")) {

            if (!p.hasPermission("qsmarket.history")) {
                Lang.msg(p, "no_permission");
                return true;
            }

            if (args.length >= 2) {
                if (!p.hasPermission("qsmarket.ah.admin")) {
                    Lang.msg(p, "no_permission");
                    return true;
                }
                java.util.UUID target = resolveTarget(args[1]);
                if (target == null) {
                    p.sendMessage("§cJugador no encontrado: " + args[1]);
                    return true;
                }
                AHHistoryMenu.open(p, target, 1);
                return true;
            }

            AHHistoryMenu.open(p, 1);
            return true;
        }

        // ==============================================================
        // /ah expired [jugador] → Items expirados (admin puede ver otros)
        // ==============================================================
        if (sub.equals("expired")) {

            if (!p.hasPermission("qsmarket.expired")) {
                Lang.msg(p, "no_permission");
                return true;
            }

            if (args.length >= 2) {
                if (!p.hasPermission("qsmarket.ah.admin")) {
                    Lang.msg(p, "no_permission");
                    return true;
                }
                java.util.UUID target = resolveTarget(args[1]);
                if (target == null) {
                    p.sendMessage("§cJugador no encontrado: " + args[1]);
                    return true;
                }
                AHExpiredMenu.open(p, target, 1);
                return true;
            }

            AHExpiredMenu.open(p, 1);
            return true;
        }

    // ==============================================================
    // /ah toggle → Cambiar entre modo admin (gestionar) y normal (comprar)
    // ==============================================================
        if (sub.equals("toggle")) {

            if (!p.hasPermission("qsmarket.ah.admin")) {
                Lang.msg(p, "no_permission");
                return true;
            }

            boolean mode = mp.quesito.qSMarketPlus.auction.holder.AHHolder.toggleAdminMode(p);
            if (mode) {
                p.sendMessage("§aModo AH: ADMINISTRADOR (gestionar/inspeccionar). Usa §f/ah toggle§a para comprar como normal.");
            } else {
                p.sendMessage("§aModo AH: NORMAL (comprar/ver como jugador). Usa §f/ah toggle§a para volver a administrar.");
            }
            return true;
        }

    // ==============================================================
    // /ah audit → Diagnóstico de subastas (admin)
    // ==============================================================
        if (sub.equals("audit")) {

            if (!p.hasPermission("qsmarket.ah.admin")) {
                Lang.msg(p, "no_permission");
                return true;
            }

            runAudit(p);
            return true;
        }

    // ==============================================================
    // /ah sell <precio>
    // ==============================================================
        if (sub.equals("sell")) {

            if (args.length < 2) {
                Lang.msg(p, "ah_price_error");
                return true;
            }

            double price;
            try {
                price = Double.parseDouble(args[1]);
            } catch (Exception ex) {
                Lang.msg(p, "ah_price_error");
                return true;
            }

            ItemStack hand = p.getInventory().getItemInMainHand();

            if (hand == null || hand.getType().isAir()) {
                Lang.msg(p, "ah_no_item");
                return true;
            }

            // =========================
            //   LIMITE DE SUBASTAS
            // =========================
            int limit = plugin.getAuctionManager().getAuctionLimit(p);
            int current = plugin.getAuctionManager().getActiveAuctions(p);

            if (current >= limit) {
                p.sendMessage("§cHas alcanzado tu límite de subastas activas.");
                p.sendMessage("§7Tu límite actual es: §e" + limit);
                return true;
            }

            int slot = p.getInventory().getHeldItemSlot();
            ItemStack toSell = hand.clone();

            AHConfirmMenu.openSingle(p, toSell, price, slot);
            return true;
        }

        // ==============================================================
        // /ah sellinv <precio>
        // ==============================================================
        if (sub.equals("sellinv")) {

            if (args.length < 2) {
                Lang.msg(p, "ah_price_error");
                return true;
            }

            double price;
            try {
                price = Double.parseDouble(args[1]);
            } catch (Exception ex) {
                Lang.msg(p, "ah_price_error");
                return true;
            }

            ItemStack[] items = Arrays.stream(p.getInventory().getStorageContents())
                    .filter(i -> i != null && !i.getType().isAir())
                    .map(ItemStack::clone)
                    .toArray(ItemStack[]::new);

            if (items.length == 0) {
                Lang.msg(p, "ah_no_item");
                return true;
            }

            // =========================
            //   LIMITE DE SUBASTAS
            // =========================
            int limit = plugin.getAuctionManager().getAuctionLimit(p);
            int current = plugin.getAuctionManager().getActiveAuctions(p);

            if (current >= limit) {
                p.sendMessage("§cHas alcanzado tu límite de subastas activas.");
                p.sendMessage("§7Tu límite actual es: §e" + limit);
                return true;
            }

            AHConfirmMenu.openBulk(p, items, price);
            return true;
        }


        // ==============================================================
        // AYUDA
        // ==============================================================
        p.sendMessage("§eUso:");
        p.sendMessage("§f/ah §7- Abrir casa de subastas");
        p.sendMessage("§f/ah sell <precio> §7- Subastar item en mano");
        p.sendMessage("§f/ah sellinv <precio> §7- Subastar todo el inventario");
        p.sendMessage("§f/ah history [jugador] §7- Ver historial de subastas");
        p.sendMessage("§f/ah expired [jugador] §7- Ver subastas expiradas");
        if (p.hasPermission("qsmarket.ah.admin")) {
            p.sendMessage("§f/ah toggle §7- Cambiar entre modo admin y modo normal (comprar)");
            p.sendMessage("§f/ah audit §7- Diagnóstico de subastas (fantasmas, vencidas, corruptas)");
            p.sendMessage("§7(Admin) Añade un [jugador] para ver las suyas; click en un item para gestionarlo.");
        }

        return true;
    }

    /* ===================== UTILS ===================== */

    private void runAudit(Player p) {
        SQLManager sql = plugin.getSqlManager();
        long now = System.currentTimeMillis();

        sql.query("SELECT status, COUNT(*) AS cnt FROM auctions GROUP BY status", rs -> {
            try {
                p.sendMessage("§e--- Auditoría AH ---");
                int total = 0;
                while (rs.next()) {
                    String st = rs.getString("status");
                    int cnt = rs.getInt("cnt");
                    total += cnt;
                    p.sendMessage("§7  " + st + ": §f" + cnt);
                }
                p.sendMessage("§7  Total filas: §e" + total);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Set<Integer> memoryIds = new HashSet<>();
        for (AuctionItem a : plugin.getAuctionManager().getAuctions()) memoryIds.add(a.id);

        List<String> ghosts = new ArrayList<>();
        List<String> pastDue = new ArrayList<>();
        List<String> corrupt = new ArrayList<>();

        sql.query("SELECT id, expires, item, container FROM auctions WHERE status = 'ACTIVE'", rs -> {
            try {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    long expires = rs.getLong("expires");

                    if (!memoryIds.contains(id)) ghosts.add(String.valueOf(id));
                    if (expires < now) pastDue.add(String.valueOf(id));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        sql.query("""
                SELECT id FROM auctions
                WHERE (item IS NULL OR item = '')
                  AND (container IS NULL OR container = '' OR container = '[]')
                """, rs -> {
            try {
                while (rs.next()) corrupt.add(String.valueOf(rs.getInt("id")));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        p.sendMessage("§7ACTIVE en SQL pero NO en memoria (fantasmas): " + (ghosts.isEmpty() ? "§anada" : "§c" + ghosts));
        p.sendMessage("§7ACTIVE ya vencidas (expirarán en la próxima revisión): " + (pastDue.isEmpty() ? "§anada" : "§e" + pastDue));
        p.sendMessage("§7Corruptas (sin item ni container, se verían como marcador): " + (corrupt.isEmpty() ? "§anada" : "§c" + corrupt));
        p.sendMessage("§7En memoria (activas): §e" + memoryIds.size());

        if (!ghosts.isEmpty() || !corrupt.isEmpty()) {
            p.sendMessage("§aConsejo: /qsmarket reload recarga el AH desde SQL; un admin puede eliminar corruptas desde el panel.");
        }
    }

    private java.util.UUID resolveTarget(String name) {
        Player online = org.bukkit.Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();

        org.bukkit.OfflinePlayer off = org.bukkit.Bukkit.getOfflinePlayer(name);
        if (off.hasPlayedBefore() || off.getName() != null) return off.getUniqueId();
        return null;
    }
}
