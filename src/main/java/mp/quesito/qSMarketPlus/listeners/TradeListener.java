package mp.quesito.qSMarketPlus.listeners;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.trade.TradeHistoryHolder;
import mp.quesito.qSMarketPlus.trade.TradeHistoryMenu;
import mp.quesito.qSMarketPlus.trade.TradeHolder;
import mp.quesito.qSMarketPlus.trade.TradeManager;
import mp.quesito.qSMarketPlus.trade.TradeMenu;
import mp.quesito.qSMarketPlus.trade.TradeSession;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import mp.quesito.qSMarketPlus.utils.MetaUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class TradeListener implements Listener {

    private final TradeManager manager;

    public TradeListener() {
        this.manager = QSMarketPlus.getInstance().getTradeManager();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof TradeHolder holder)) return;

        TradeSession session = holder.getSession();
        if (session == null || session.isFinalizing()) {
            e.setCancelled(true);
            return;
        }

        // Doble click: recoge objetos iguales de TODO el menu, incluida la oferta del otro jugador
        if (e.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            e.setCancelled(true);
            return;
        }

        // Solo los participantes pueden interactuar
        if (!session.isParticipant(player.getUniqueId())) {
            e.setCancelled(true);
            return;
        }

        // Click fuera (drop de items a tierra)
        if (e.getClickedInventory() == null) {
            e.setCancelled(true);
            return;
        }

        boolean isA = session.isPlayerA(player.getUniqueId());
        int[] mySlots = isA ? TradeMenu.A_OFFER : TradeMenu.B_OFFER;
        int myReady = isA ? TradeMenu.A_READY : TradeMenu.B_READY;

        // =============================================
        //  CLICK EN EL MENÚ DE TRADEO (top)
        // =============================================
        if (e.getClickedInventory() == top) {

            int slot = e.getSlot();

            if (slot == TradeMenu.CANCEL) {
                e.setCancelled(true);
                manager.cancelTrade(session, player, "trade.cancelled-by");
                return;
            }

            if (slot == myReady) {
                e.setCancelled(true);
                manager.toggleReady(session, player);
                return;
            }

            // ===== Botones de moneda / niveles =====
            boolean isMoneySlot = slot == TradeMenu.A_MONEY || slot == TradeMenu.B_MONEY;
            boolean isXpSlot = slot == TradeMenu.A_XP || slot == TradeMenu.B_XP;

            if (isMoneySlot || isXpSlot) {
                e.setCancelled(true);

                boolean mine = isA
                        ? (slot == TradeMenu.A_MONEY || slot == TradeMenu.A_XP)
                        : (slot == TradeMenu.B_MONEY || slot == TradeMenu.B_XP);

                if (!mine) return;

                manager.offerChanged(session, player);

                String kind = isXpSlot ? "xp" : "money";

                // Click derecho -> quitar la oferta
                if (e.isRightClick()) {
                    if (isA) {
                        if (kind.equals("money")) session.setMoneyA(0);
                        else session.setXpA(0);
                    } else {
                        if (kind.equals("money")) session.setMoneyB(0);
                        else session.setXpB(0);
                    }
                    TradeMenu.render(session.getInventory(), session);
                    return;
                }

                // Click izquierdo -> escribir la cantidad en el chat
                manager.setPendingInput(player, session, kind);
                return;
            }

            if (contains(mySlots, slot)) {
                // Permitir colocar/quitar nuestros propios ítems
                boolean nothing = (e.getCurrentItem() == null || e.getCurrentItem().getType().isAir())
                        && (e.getCursor() == null || e.getCursor().getType().isAir())
                        && e.getHotbarButton() < 0;

                if (!nothing) {
                    manager.offerChanged(session, player);
                }
                return;
            }

            e.setCancelled(true);
            return;
        }

        // =============================================
        //  CLICK EN NUESTRO INVENTARIO (bottom)
        // =============================================
        if (e.isShiftClick()) {
            e.setCancelled(true);

            ItemStack current = e.getCurrentItem();
            if (current == null || current.getType().isAir()) return;

            manager.offerChanged(session, player);

            int clickedSlot = e.getSlot();
            shiftAddToOffer(player, top, mySlots, player.getInventory().getItem(clickedSlot), clickedSlot);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        TradeManager.PendingInput pending = manager.getPendingInput(player.getUniqueId());
        if (pending == null) return;

        // El jugador está escribiendo una cantidad para el tradeo
        e.setCancelled(true);

        final String msg = e.getMessage();

        // El estado del juego debe modificarse en el hilo principal
        Bukkit.getScheduler().runTask(QSMarketPlus.getInstance(),
                () -> manager.handleChatInput(player, msg));
    }

    @EventHandler
    public void onHistoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof TradeHistoryHolder holder)) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String btn = MetaUtil.getTag(clicked.getItemMeta(), "btn");
        if (btn == null) return;

        switch (btn) {
            case "prev" -> TradeHistoryMenu.open(player, holder.getPage() - 1);
            case "next" -> TradeHistoryMenu.open(player, holder.getPage() + 1);
            case "close" -> player.closeInventory();
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!(e.getView().getTopInventory().getHolder() instanceof TradeHolder)) return;

        // Cancelar cualquier arrastre que toque el menú de tradeo
        int topSize = e.getView().getTopInventory().getSize();
        for (int raw : e.getRawSlots()) {
            if (raw < topSize) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        if (!(e.getView().getTopInventory().getHolder() instanceof TradeHolder holder)) return;

        TradeSession session = holder.getSession();
        if (session == null || session.isFinalizing()) return;

        // Si cerró el inventario para escribir la cantidad en el chat, NO cancelar
        if (session.isInputting(player.getUniqueId())) return;

        // El jugador cerró el tradeo -> cancelar para ambos
        manager.cancelTrade(session, player, "trade.cancelled-by");
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        TradeSession session = manager.getSession(e.getEntity().getUniqueId());
        if (session != null) {
            manager.cancelTrade(session, e.getEntity(), "trade.cancelled-by");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        TradeSession session = manager.getSession(player.getUniqueId());
        if (session != null) {
            manager.cancelTrade(session, player, "trade.cancelled-by");
        }
    }

    // =====================================================
    //  COLOCAR ÍTEM SHIFT-CLICK EN NUESTRA ÁREA DE OFERTA
    // =====================================================
    private void shiftAddToOffer(Player player, Inventory top, int[] offerSlots, ItemStack current, int clickedSlot) {
        if (current == null || current.getType().isAir()) return;

        int needed = current.getAmount();

        for (int offerSlot : offerSlots) {
            if (needed <= 0) break;

            ItemStack offer = top.getItem(offerSlot);

            if (offer == null || offer.getType().isAir()) {
                ItemStack copy = current.clone();
                copy.setAmount(needed);
                top.setItem(offerSlot, copy);
                needed = 0;
            } else if (offer.isSimilar(current)) {
                int canAdd = Math.min(needed, offer.getMaxStackSize() - offer.getAmount());
                if (canAdd > 0) {
                    offer.setAmount(offer.getAmount() + canAdd);
                    top.setItem(offerSlot, offer);
                    needed -= canAdd;
                }
            }
        }

        // Nada se movió -> oferta llena
        if (needed == current.getAmount()) {
            MessageUtil.lang(player, "trade.offer-full");
            return;
        }

        // Actualizar el slot de nuestro inventario
        if (needed <= 0) {
            player.getInventory().setItem(clickedSlot, null);
        } else {
            ItemStack rem = current.clone();
            rem.setAmount(needed);
            player.getInventory().setItem(clickedSlot, rem);
        }
    }

    private boolean contains(int[] slots, int slot) {
        for (int s : slots) {
            if (s == slot) return true;
        }
        return false;
    }
}
