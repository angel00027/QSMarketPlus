package mp.quesito.qSMarketPlus.shop.browser;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.pwarp.PwarpSafety;
import mp.quesito.qSMarketPlus.shop.PlayerShop;
import mp.quesito.qSMarketPlus.shop.browser.ShopBrowser.Offer;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import mp.quesito.qSMarketPlus.utils.MetaUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Clics del buscador y viaje seguro a las tiendas.
 *
 * Seguro: se busca un sitio con suelo firme y aire para cuerpo y cabeza junto al
 * cartel (sin lava, cactus ni fuego), el jugador espera quieto unos segundos (se
 * cancela si se mueve o recibe dano), hay enfriamiento y llega mirando al cofre.
 */
public class ShopBrowserListener implements Listener {

    private final Map<UUID, BukkitTask> pending = new HashMap<>();
    private final Map<UUID, Location> pendingFrom = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    @EventHandler
    public void onClick(InventoryClickEvent e) {

        if (!(e.getView().getTopInventory().getHolder() instanceof ShopBrowserHolder holder)) return;
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String btn = MetaUtil.getTag(clicked.getItemMeta(), "btn");
        String product = MetaUtil.getTag(clicked.getItemMeta(), "product");
        String shopLoc = MetaUtil.getTag(clicked.getItemMeta(), "shop_loc");

        boolean inProduct = holder.getProduct() >= 0;

        if (btn != null) {
            switch (btn) {
                case "close" -> player.closeInventory();
                case "back" -> ShopBrowser.openMain(player, 1);
                case "prev", "next" -> {
                    int page = holder.getPage() + ("next".equals(btn) ? 1 : -1);
                    if (inProduct) ShopBrowser.openProduct(player, holder.getProducts(), holder.getProduct(), page);
                    else ShopBrowser.openMain(player, page);
                }
                default -> { }
            }
            return;
        }

        if (product != null) {
            ShopBrowser.openProduct(player, holder.getProducts(), Integer.parseInt(product), 1);
            return;
        }

        if (shopLoc != null && inProduct) {
            for (Offer offer : holder.getProducts().get(holder.getProduct()).offers()) {
                if (ShopBrowser.key(offer.shop().getChestLocation()).equals(shopLoc)) {
                    player.closeInventory();
                    startTeleport(player, offer.shop());
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof ShopBrowserHolder) e.setCancelled(true);
    }

    // =====================================================
    //                 VIAJE SEGURO
    // =====================================================
    private void startTeleport(Player player, PlayerShop shop) {

        if (!player.hasPermission("qsmarket.tiendas.tp")) {
            msg(player, "no-permission", "<red>No puedes viajar a las tiendas.");
            return;
        }

        UUID uuid = player.getUniqueId();
        long cooldownMs = cfg("cooldown-seconds", 10) * 1000L;
        long left = cooldowns.getOrDefault(uuid, 0L) + cooldownMs - System.currentTimeMillis();
        if (left > 0) {
            msg(player, "cooldown", "<red>Espera <seconds>s para volver a viajar.",
                    "seconds", String.valueOf((left + 999) / 1000));
            return;
        }

        cancel(uuid);

        int warmup = cfg("warmup-seconds", 3);
        pendingFrom.put(uuid, player.getLocation().clone());

        final int[] remaining = {warmup};
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(QSMarketPlus.getInstance(), () -> {
            if (!player.isOnline()) {
                cancel(uuid);
                return;
            }
            if (remaining[0] <= 0) {
                cancel(uuid);
                teleport(player, shop);
                return;
            }
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(MessageUtil.toLegacy(text("warmup",
                    "<yellow>Viajando a la tienda en <white><seconds>s</white>... no te muevas")
                    .replace("<seconds>", String.valueOf(remaining[0])))));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, 1.6f);
            remaining[0]--;
        }, 0L, 20L);

        pending.put(uuid, task);
    }

    private void teleport(Player player, PlayerShop shop) {

        Location chest = shop.getChestLocation();
        Location sign = shop.getSignLocation();

        // La tienda pudo romperse mientras esperaba
        if (shop.getChestInventory() == null) {
            msg(player, "gone", "<red>Esa tienda ya no existe.");
            ShopBrowser.invalidate();
            return;
        }

        Location preferred = sign != null && sign.getWorld() != null ? sign : chest;
        Location safe = PwarpSafety.findSafeLocation(preferred);
        if (safe == null) {
            msg(player, "unsafe", "<red>No hay un sitio seguro junto a esa tienda. Avisa a su dueño.");
            return;
        }

        // Llegar mirando al cofre
        Location center = chest.clone().add(0.5, 0.5, 0.5);
        Vector look = center.toVector().subtract(safe.clone().add(0, 1.62, 0).toVector());
        safe.setDirection(look);

        player.setFallDistance(0f);
        player.teleport(safe);
        player.playSound(safe, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.2f);
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        msg(player, "arrived", "<green>Llegaste a la tienda de <white><owner></white>.",
                "owner", shop.getOwnerName() != null ? shop.getOwnerName() : "Desconocido");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Location from = pendingFrom.get(e.getPlayer().getUniqueId());
        if (from == null || e.getTo() == null) return;

        if (from.getWorld() != e.getTo().getWorld() || from.distanceSquared(e.getTo()) > 0.25) {
            cancel(e.getPlayer().getUniqueId());
            msg(e.getPlayer(), "moved", "<red>Viaje cancelado: te moviste.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player player && pending.containsKey(player.getUniqueId())) {
            cancel(player.getUniqueId());
            msg(player, "damaged", "<red>Viaje cancelado: recibiste daño.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        cancel(e.getPlayer().getUniqueId());
    }

    private void cancel(UUID uuid) {
        BukkitTask task = pending.remove(uuid);
        if (task != null) task.cancel();
        pendingFrom.remove(uuid);
    }

    // =====================================================
    private static int cfg(String key, int def) {
        return Math.max(0, ShopBrowser.cfg().getInt("player-shops-browser." + key, def));
    }

    private static String text(String key, String fallback) {
        return ShopBrowser.cfg().getString("player-shops-browser.messages." + key, fallback);
    }

    private static void msg(Player player, String key, String fallback, String... vars) {
        String raw = text(key, fallback);
        for (int i = 0; i + 1 < vars.length; i += 2) {
            raw = raw.replace("<" + vars[i] + ">", vars[i + 1]);
        }
        player.sendMessage(MessageUtil.toLegacy(raw));
    }
}
