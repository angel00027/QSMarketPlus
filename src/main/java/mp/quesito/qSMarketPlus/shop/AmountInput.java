package mp.quesito.qSMarketPlus.shop;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.holder.AmountHolder;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Boton "Escribir cantidad": cierra el menu, lee el numero del chat y lo vuelve a abrir. */
public class AmountInput implements Listener {

    private static final long TIMEOUT_TICKS = 20L * 30L;

    private static final Map<UUID, AmountHolder> waiting = new ConcurrentHashMap<>();

    public static void start(Player player, AmountHolder holder) {

        waiting.put(player.getUniqueId(), holder);
        player.closeInventory();

        send(player, "texts.write-prompt", "<yellow>Escribe la cantidad en el chat. Máximo: <white><max></white>. "
                + "<gray>Escribe <white>cancelar</white> para volver.", AmountMenu.maxAmount(player, holder));

        Bukkit.getScheduler().runTaskLater(QSMarketPlus.getInstance(), () -> {
            if (waiting.remove(player.getUniqueId(), holder) && player.isOnline()) {
                AmountMenu.open(player, holder);
            }
        }, TIMEOUT_TICKS);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {

        Player player = e.getPlayer();
        AmountHolder holder = waiting.remove(player.getUniqueId());
        if (holder == null) return;

        e.setCancelled(true);

        String message = e.getMessage().trim();

        // El inventario solo se puede tocar en el hilo principal
        Bukkit.getScheduler().runTask(QSMarketPlus.getInstance(), () -> apply(player, holder, message));
    }

    private void apply(Player player, AmountHolder holder, String message) {

        if (!player.isOnline()) return;

        int max = AmountMenu.maxAmount(player, holder);

        if (message.equalsIgnoreCase("max")) {
            holder.setAmount(AmountMenu.clamp(max, max));

        } else if (!message.equalsIgnoreCase("cancelar") && !message.equalsIgnoreCase("cancel")) {

            int typed;
            try {
                typed = Integer.parseInt(message.replaceAll("[.,\\s]", ""));
            } catch (NumberFormatException ex) {
                typed = 0;
            }

            if (typed <= 0) {
                send(player, "texts.write-invalid", "<red>Esa cantidad no es válida.", max);
            } else {
                if (typed > max) {
                    send(player, "texts.write-capped", "<yellow>Solo puedes llegar a <white><max></white>.", max);
                }
                holder.setAmount(AmountMenu.clamp(typed, max));
            }
        }

        AmountMenu.open(player, holder);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        waiting.remove(e.getPlayer().getUniqueId());
    }

    private static void send(Player player, String path, String fallback, int max) {
        String raw = QSMarketPlus.getInstance().amountMenuConfig.getConfig().getString(path, fallback);
        player.sendMessage(MessageUtil.toLegacy(raw.replace("<max>", String.valueOf(Math.max(0, max)))));
    }
}
