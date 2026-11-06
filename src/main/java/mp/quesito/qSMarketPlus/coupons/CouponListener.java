package mp.quesito.qSMarketPlus.coupons;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.coupons.CouponManager.Redeem;
import mp.quesito.qSMarketPlus.utils.MetaUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CouponListener implements Listener {

    private static final Set<UUID> typing = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> lastTry = new ConcurrentHashMap<>();

    private CouponManager manager() {
        return QSMarketPlus.getInstance().getCouponManager();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof CouponMenu.Holder holder)) return;
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String btn = MetaUtil.getTag(clicked.getItemMeta(), "btn");
        String code = MetaUtil.getTag(clicked.getItemMeta(), "coupon");

        if (code != null) {
            CouponManager.Coupon active = manager().selected(player);
            boolean wasActive = active != null && active.code().equals(code);
            manager().select(player, wasActive ? null : code);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, wasActive ? 0.8f : 1.4f);
            CouponMenu.open(player, holder.getPage());
            return;
        }

        if (btn == null) return;
        switch (btn) {
            case "close" -> player.closeInventory();
            case "prev" -> CouponMenu.open(player, holder.getPage() - 1);
            case "next" -> CouponMenu.open(player, holder.getPage() + 1);
            case "deactivate" -> {
                manager().select(player, null);
                CouponMenu.open(player, holder.getPage());
            }
            case "redeem" -> {
                typing.add(player.getUniqueId());
                player.closeInventory();
                manager().send(player, "escribe-codigo", "<yellow>Escribe el código en el chat. <gray>(<white>cancelar</white> para salir)");
                Bukkit.getScheduler().runTaskLater(QSMarketPlus.getInstance(), () -> {
                    if (typing.remove(player.getUniqueId()) && player.isOnline()) {
                        manager().send(player, "tiempo-agotado",
                                "<gray>Se acabó el tiempo para escribir el código. Vuelve a pulsar Canjear.");
                    }
                }, 20L * 60);
            }
            default -> { }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof CouponMenu.Holder) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        if (!typing.remove(player.getUniqueId())) return;

        e.setCancelled(true);
        String message = e.getMessage().trim();

        Bukkit.getScheduler().runTask(QSMarketPlus.getInstance(), () -> {
            if (!message.equalsIgnoreCase("cancelar")) redeem(player, message);
            CouponMenu.open(player, 1);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        typing.remove(e.getPlayer().getUniqueId());
    }

    /** Canje compartido por el menu y por /cupon canjear. Tiene una pausa para no probar codigos a lo loco. */
    public static void redeem(Player player, String code) {
        CouponManager manager = QSMarketPlus.getInstance().getCouponManager();

        long now = System.currentTimeMillis();
        if (now - lastTry.getOrDefault(player.getUniqueId(), 0L) < 2000L) {
            manager.send(player, "espera", "<red>Espera un momento antes de probar otro código.");
            return;
        }
        lastTry.put(player.getUniqueId(), now);

        Redeem result = manager.redeem(player, code);
        switch (result.result()) {
            case OK -> {
                CouponManager.Template t = manager.template(result.coupon().template());
                manager.send(player, "canjeado", "<green>✔ Cupón canjeado: <white><name></white> (<percent>% en <categories>)",
                        "name", t.name(),
                        "percent", CouponManager.money(t.percent()),
                        "categories", manager.categoryNames(t));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.6f);
            }
            case NOT_FOUND -> manager.send(player, "no-existe", "<red>Ese código no existe.");
            case TAKEN -> manager.send(player, "ya-usado", "<red>Ese código ya lo canjeó otra persona.");
            case EXPIRED -> manager.send(player, "caducado", "<red>Ese cupón ya caducó o no tiene usos.");
            case LIMIT -> manager.send(player, "agotado", "<red>Ese código ya se agotó.");
            case ALREADY -> manager.send(player, "ya-lo-tienes", "<yellow>Ya canjeaste ese código.");
        }
    }
}
