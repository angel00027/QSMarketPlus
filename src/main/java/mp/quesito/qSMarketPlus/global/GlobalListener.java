package mp.quesito.qSMarketPlus.global;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import mp.quesito.qSMarketPlus.utils.MetaUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GlobalListener implements Listener {

    private final QSMarketPlus plugin;
    private final Set<UUID> pendingInput = new HashSet<>();

    public GlobalListener(QSMarketPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof GlobalHolder holder)) return;
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player p)) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();

        String btn = MetaUtil.getTag(meta, "btn");
        if (btn != null) {
            switch (btn) {
                case "close" -> p.closeInventory();
                case "next" -> GlobalMenu.open(p, holder.getTab(), holder.getPlayerFilter(),
                        holder.getPlayerFilterName(), holder.getTimeIndex(), holder.getPage() + 1);
                case "prev" -> GlobalMenu.open(p, holder.getTab(), holder.getPlayerFilter(),
                        holder.getPlayerFilterName(), holder.getTimeIndex(), holder.getPage() - 1);
            }
            return;
        }

        String tab = MetaUtil.getTag(meta, "tab");
        if (tab != null) {
            try {
                GlobalMenu.open(p, Integer.parseInt(tab), holder.getPlayerFilter(),
                        holder.getPlayerFilterName(), holder.getTimeIndex(), 1);
            } catch (NumberFormatException ignored) { }
            return;
        }

        String time = MetaUtil.getTag(meta, "time");
        if (time != null) {
            try {
                int cur = Integer.parseInt(time);
                GlobalMenu.open(p, holder.getTab(), holder.getPlayerFilter(),
                        holder.getPlayerFilterName(), (cur + 1) % 4, 1);
            } catch (NumberFormatException ignored) { }
            return;
        }

        String filter = MetaUtil.getTag(meta, "filter");
        if (filter != null) {
            pendingInput.add(p.getUniqueId());
            p.closeInventory();
            MessageUtil.msg(p, "<yellow>Escribe en el chat el nombre del jugador a filtrar.");
            MessageUtil.msg(p, "<gray>Escribe <white>off</white> para quitar el filtro de jugador.");
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (!pendingInput.remove(p.getUniqueId())) return;
        e.setCancelled(true);

        String message = e.getMessage() == null ? "" : e.getMessage().trim();

        // El jugador tiene que seguir con el menú abierto
        Inventory inv = p.getOpenInventory().getTopInventory();
        if (!(inv.getHolder() instanceof GlobalHolder holder)) return;

        UUID filter = holder.getPlayerFilter();
        String filterName = holder.getPlayerFilterName();
        int tab = holder.getTab();
        int time = holder.getTimeIndex();

        if (message.equalsIgnoreCase("off") || message.equalsIgnoreCase("cancelar")) {
            MessageUtil.msg(p, "<green>Filtro de jugador desactivado.");
            GlobalMenu.open(p, tab, null, null, time, 1);
            return;
        }

        Player online = Bukkit.getPlayerExact(message);
        if (online != null) {
            GlobalMenu.open(p, tab, online.getUniqueId(), online.getName(), time, 1);
            MessageUtil.msg(p, "<green>Filtrando por: <white>" + online.getName());
            return;
        }

        final String[] found = {null};
        plugin.getSqlManager().query("""
                SELECT player_uuid FROM shop_transactions
                WHERE LOWER(player_name) = LOWER(?)
                ORDER BY id DESC LIMIT 1
                """, rs -> {
            try {
                if (rs.next()) found[0] = rs.getString("player_uuid");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, message);

        if (found[0] == null) {
            MessageUtil.msg(p, "<red>No se encontró ningún jugador con ese nombre en el historial.");
            GlobalMenu.open(p, tab, filter, filterName, time, 1);
            return;
        }

        try {
            UUID uuid = UUID.fromString(found[0]);
            GlobalMenu.open(p, tab, uuid, message, time, 1);
            MessageUtil.msg(p, "<green>Filtrando por: <white>" + message);
        } catch (IllegalArgumentException ex) {
            MessageUtil.msg(p, "<red>UUID inválido en el historial.");
            GlobalMenu.open(p, tab, filter, filterName, time, 1);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        pendingInput.remove(p.getUniqueId());
    }
}