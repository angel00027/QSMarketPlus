package mp.quesito.qSMarketPlus.trade;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.economia.EconomyProvider;
import mp.quesito.qSMarketPlus.utils.Lang;
import mp.quesito.qSMarketPlus.utils.XpUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TradeManager {

    private final QSMarketPlus plugin;

    private final TradeDAO dao;

    // Solicitudes pendientes: key = UUID del destinatario
    private final Map<UUID, TradeRequest> requests = new HashMap<>();

    // Sessions activas: key = UUID del jugador -> session
    private final Map<UUID, TradeSession> sessions = new HashMap<>();

    // Jugadores escribiendo la cantidad en el chat (lectura desde hilo async)
    private final Map<UUID, PendingInput> pendingInputs = new ConcurrentHashMap<>();

    public record PendingInput(TradeSession session, String kind, long expiresAt) {
    }

    public TradeManager(QSMarketPlus plugin) {
        this.plugin = plugin;
        this.dao = new TradeDAO(plugin.getSqlManager());
    }

    public TradeDAO getDao() {
        return dao;
    }

    // =====================================================
    //                  SOLICITUDES
    // =====================================================
    public void request(Player from, Player to) {

        if (from.equals(to)) {
            Lang.msg(from, "trade.self");
            return;
        }

        if (isInTrade(from)) {
            Lang.msg(from, "trade.in-trade");
            return;
        }

        if (isInTrade(to)) {
            Lang.msg(from, "trade.busy");
            return;
        }

        if (!to.hasPermission("qsmarket.trade")) {
            Lang.msg(from, "trade.no-permission-target");
            return;
        }

        if (!inRange(from, to)) {
            Lang.msg(from, "trade.too-far",
                    "player", to.getName(),
                    "distance", maxDistance());
            return;
        }

        removeExpired();

        // Si ambas partes se solicitaron mutuamente, tradeo directo
        TradeRequest reverse = requests.get(from.getUniqueId());
        if (reverse != null && reverse.getFrom().equals(to.getUniqueId())) {
            requests.remove(from.getUniqueId());
            startTrade(from, to);
            return;
        }

        requests.put(to.getUniqueId(), new TradeRequest(from.getUniqueId(), to.getUniqueId()));

        Lang.msg(from, "trade.request-sent", "player", to.getName());
        Lang.msg(to, "trade.request-received", "player", from.getName());
    }

    public void accept(Player acceptor, Player other) {

        if (isInTrade(acceptor)) {
            Lang.msg(acceptor, "trade.in-trade");
            return;
        }

        if (isInTrade(other)) {
            Lang.msg(acceptor, "trade.busy");
            return;
        }

        if (!inRange(acceptor, other)) {
            Lang.msg(acceptor, "trade.too-far",
                    "player", other.getName(),
                    "distance", maxDistance());
            return;
        }

        removeExpired();

        TradeRequest req = requests.remove(acceptor.getUniqueId());
        if (req == null || !req.getFrom().equals(other.getUniqueId())) {
            Lang.msg(acceptor, "trade.no-request", "player", other.getName());
            return;
        }

        startTrade(acceptor, other);
    }

    public void deny(Player denier, Player other) {

        removeExpired();

        TradeRequest req = requests.remove(denier.getUniqueId());
        if (req == null || !req.getFrom().equals(other.getUniqueId())) {
            Lang.msg(denier, "trade.no-request", "player", other.getName());
            return;
        }

        Lang.msg(denier, "trade.denied", "player", other.getName());
        Lang.msg(other, "trade.sender-denied", "player", denier.getName());
    }

    // =====================================================
    //                  INICIAR TRADEO
    // =====================================================
    public void startTrade(Player a, Player b) {

        if (isInTrade(a)) {
            Lang.msg(a, "trade.in-trade");
            return;
        }

        if (isInTrade(b)) {
            Lang.msg(b, "trade.busy");
            return;
        }

        if (!inRange(a, b)) {
            Lang.msg(a, "trade.too-far",
                    "player", b.getName(),
                    "distance", maxDistance());
            Lang.msg(b, "trade.too-far",
                    "player", a.getName(),
                    "distance", maxDistance());
            return;
        }

        TradeSession session = new TradeSession(a.getUniqueId(), b.getUniqueId());
        sessions.put(a.getUniqueId(), session);
        sessions.put(b.getUniqueId(), session);

        TradeMenu.open(session);

        // Otro plugin pudo impedir abrir el menu: sin esto quedarian atrapados en un tradeo invisible
        if (!isViewing(a, session) || !isViewing(b, session)) {
            cancelTrade(session, null, null);
            return;
        }

        Lang.msg(a, "trade.started", "player", b.getName());
        Lang.msg(b, "trade.started", "player", a.getName());

        a.playSound(a.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
        b.playSound(b.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
    }

    // =====================================================
    //                  TOGGLE LISTO / COMPLETAR
    // =====================================================
    public void toggleReady(TradeSession session, Player player) {
        if (session == null || session.isFinalizing()) return;

        boolean isA = session.isPlayerA(player.getUniqueId());
        if (isA) {
            session.setReadyA(!session.isReadyA());
        } else {
            session.setReadyB(!session.isReadyB());
        }

        if (session.isReadyA() && session.isReadyB()) {
            startCountdown(session);
        } else {
            // Se canceló el listo -> quitar la cuenta atrás y volver al color normal
            session.cancelCountdown();
            TradeMenu.render(session.getInventory(), session);
        }
    }

    /**
     * Cualquier cambio de oferta quita el "listo" de LOS DOS. Si solo se quitara el de quien
     * cambia, podria cambiar diamantes por tierra y volver a confirmar con el otro aun listo.
     */
    public void offerChanged(TradeSession session, Player changer) {
        boolean otherWasReady = session.isReady(session.getOther(changer.getUniqueId()));

        session.setReadyA(false);
        session.setReadyB(false);
        session.cancelCountdown();
        TradeMenu.render(session.getInventory(), session);

        if (otherWasReady) {
            Player other = Bukkit.getPlayer(session.getOther(changer.getUniqueId()));
            if (other != null) {
                Lang.msg(other, "trade.offer-changed", "player", changer.getName());
                other.playSound(other.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.8f);
            }
        }

        // El contador de objetos se lee despues de que el click mueva el objeto
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!session.isFinalizing()) {
                TradeMenu.render(session.getInventory(), session);
            }
        });
    }

    // Ambos jugadores están listos: cuenta atrás para completar,
    // con tiempo de cancelar si quieren añadir algo más.
    public void startCountdown(TradeSession session) {
        if (session == null || session.isFinalizing()) return;

        session.cancelCountdown();
        session.setCountdown(countdownSeconds());

        TradeMenu.render(session.getInventory(), session);

        session.setCountdownTask(Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (session.isFinalizing() || !(session.isReadyA() && session.isReadyB())) {
                session.cancelCountdown();
                return;
            }

            int left = session.getCountdown();
            if (left <= 0) {
                session.cancelCountdown();
                completeTrade(session);
                return;
            }

            session.setCountdown(left - 1);
            TradeMenu.render(session.getInventory(), session);

            if (left - 1 <= 0) {
                session.cancelCountdown();
                completeTrade(session);
            }
        }, 20L, 20L));
    }

    private int countdownSeconds() {
        return Math.max(1, plugin.getConfig().getInt("trade.countdown-seconds", 5));
    }

    public void completeTrade(TradeSession session) {
        if (session == null || session.isFinalizing()) return;

        session.cancelCountdown();

        Player a = Bukkit.getPlayer(session.getPlayerA());
        Player b = Bukkit.getPlayer(session.getPlayerB());

        if (a == null || b == null) {
            cancelTrade(session, a != null ? a : b, "trade.cancelled-by");
            return;
        }

        if (!inRange(a, b)) {
            session.setReadyA(false);
            session.setReadyB(false);
            TradeMenu.render(session.getInventory(), session);

            Lang.msg(a, "trade.too-far",
                    "player", b.getName(),
                    "distance", maxDistance());
            Lang.msg(b, "trade.too-far",
                    "player", a.getName(),
                    "distance", maxDistance());
            return;
        }

        Inventory inv = session.getInventory();
        if (inv == null) {
            removeSession(session);
            return;
        }

        // Verificar fondos de moneda/XP
        if (!canAfford(session, a, b)) {
            session.setReadyA(false);
            session.setReadyB(false);
            TradeMenu.render(inv, session);

            Lang.msg(a, "trade.no-funds");
            Lang.msg(b, "trade.no-funds");
            return;
        }

        List<ItemStack> aItems = TradeMenu.readOffers(inv, TradeMenu.A_OFFER);
        List<ItemStack> bItems = TradeMenu.readOffers(inv, TradeMenu.B_OFFER);

        // Comprobar que cada uno puede recibir lo que le toca
        boolean aFits = fits(a, bItems);
        boolean bFits = fits(b, aItems);

        if (!aFits || !bFits) {
            session.setReadyA(false);
            session.setReadyB(false);
            TradeMenu.render(inv, session);

            Lang.msg(a, aFits ? "trade.no-space-other" : "trade.no-space", "player", b.getName());
            Lang.msg(b, bFits ? "trade.no-space-other" : "trade.no-space", "player", a.getName());
            return;
        }

        // Intercambiar moneda y XP: si el cobro falla no se entrega nada
        if (!exchangeCurrency(session, a, b)) {
            session.setReadyA(false);
            session.setReadyB(false);
            TradeMenu.render(inv, session);

            Lang.msg(a, "trade.no-funds");
            Lang.msg(b, "trade.no-funds");
            return;
        }

        session.setFinalizing(true);
        clearPendingFor(session);

        // Guardar historial
        dao.insertTrade(new TradeRecord(
                a.getUniqueId(), b.getUniqueId(),
                aItems, bItems,
                session.getMoneyA(), session.getMoneyB(),
                session.getXpA(), session.getXpB(),
                System.currentTimeMillis()));

        TradeMenu.clearOffers(inv, TradeMenu.A_OFFER);
        TradeMenu.clearOffers(inv, TradeMenu.B_OFFER);

        closeIfTrade(a);
        closeIfTrade(b);

        // Item de A -> inventario de B, item de B -> inventario de A
        giveItems(b, aItems);
        giveItems(a, bItems);

        removeSession(session);

        Lang.msg(a, "trade.completed", "player", b.getName());
        Lang.msg(b, "trade.completed", "player", a.getName());

        a.playSound(a.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        b.playSound(b.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
    }

    // =====================================================
    //                  MONEDA / XP
    // =====================================================
    public void setPendingInput(Player player, TradeSession session, String kind) {
        if (session == null || session.isFinalizing()) return;

        session.setInputting(player.getUniqueId(), true);

        long timeoutMs = plugin.getConfig().getInt("trade.input-timeout", 45) * 1000L;
        PendingInput pending = new PendingInput(session, kind, System.currentTimeMillis() + timeoutMs);
        pendingInputs.put(player.getUniqueId(), pending);

        // Si no escribe nada, vuelve al tradeo en vez de quedarse fuera para siempre
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingInputs.get(player.getUniqueId()) == pending && player.isOnline()) {
                Lang.msg(player, "trade.input-cancelled");
                cancelCurrencyInput(player);
            }
        }, timeoutMs / 50L);

        // Cerrar el menú para que el jugador pueda escribir la cantidad en el chat.
        // El flag "inputting" evita que este cierre cancele el tradeo.
        player.closeInventory();

        if (kind.equals("money")) {
            EconomyProvider vault = economy("vault");
            double max = vault != null ? Math.max(0, vault.getBalance(player)) : 0;
            Lang.msg(player, "trade.input-prompt-money", "max", formatDouble(max));
        } else {
            Lang.msg(player, "trade.input-prompt-xp", "max", player.getLevel());
        }
    }

    public PendingInput getPendingInput(UUID player) {
        PendingInput pi = pendingInputs.get(player);
        if (pi == null) return null;

        return System.currentTimeMillis() > pi.expiresAt() ? null : pi;
    }

    public void removePendingInput(UUID player) {
        pendingInputs.remove(player);
    }

    // Procesar lo escrito en el chat (SE DEBE LLAMAR EN EL HILO PRINCIPAL)
    public void handleChatInput(Player player, String rawMessage) {
        PendingInput pi = pendingInputs.get(player.getUniqueId());
        if (pi == null) return;

        String msg = rawMessage.trim();

        if (msg.equalsIgnoreCase("cancelar") || msg.equalsIgnoreCase("cancel") || msg.equalsIgnoreCase("-")) {
            pendingInputs.remove(player.getUniqueId());
            Lang.msg(player, "trade.input-cancelled");
            cancelCurrencyInput(player);
            return;
        }

        double value;
        try {
            value = Double.parseDouble(msg.replace(',', '.').replace("$", "").replace(" ", ""));
        } catch (NumberFormatException ex) {
            Lang.msg(player, "trade.input-invalid");
            return;
        }

        if (!Double.isFinite(value) || value < 0) {
            Lang.msg(player, "trade.input-invalid");
            return;
        }

        applyCurrencyInput(player, pi.kind(), value);
    }

    public void applyCurrencyInput(Player player, String kind, double amount) {
        PendingInput pi = pendingInputs.remove(player.getUniqueId());
        if (pi == null || pi.session() == null || pi.session().isFinalizing()) return;

        TradeSession session = pi.session();
        boolean isA = session.isPlayerA(player.getUniqueId());

        if (kind.equals("money")) {
            EconomyProvider vault = economy("vault");
            if (vault == null) return;

            double balance = Math.max(0, vault.getBalance(player));
            double target = Math.round(Math.max(0, amount) * 100.0) / 100.0;
            if (target > balance) {
                target = balance;
                Lang.msg(player, "trade.no-money");
            }

            if (isA) session.setMoneyA(target); else session.setMoneyB(target);
        } else {
            int levels = Math.max(0, (int) amount);
            int maxLevels = player.getLevel();
            if (levels > maxLevels) {
                levels = maxLevels;
                Lang.msg(player, "trade.no-xp");
            }

            if (isA) session.setXpA(levels); else session.setXpB(levels);
        }

        offerChanged(session, player);

        // Reabrir el menú si se cerró para escribir en el chat
        reopenTradeView(player, session);
        session.setInputting(player.getUniqueId(), false);
    }

    // El jugador escribió "cancelar" o cerró la entrada
    public void cancelCurrencyInput(Player player) {
        TradeSession session = pendingInputs.containsKey(player.getUniqueId())
                ? pendingInputs.get(player.getUniqueId()).session()
                : null;
        pendingInputs.remove(player.getUniqueId());

        if (session == null) return;

        session.setInputting(player.getUniqueId(), false);
        reopenTradeView(player, session);
    }

    // Reabre el menú de tradeo del jugador solo si su vista está cerrada
    private void reopenTradeView(Player player, TradeSession session) {
        if (player == null || !player.isOnline()) return;
        if (session == null || session.isFinalizing()) return;

        Inventory open = player.getOpenInventory().getTopInventory();
        boolean viewing = open != null && open.getHolder() instanceof TradeHolder holder
                && holder.getSession() == session;

        if (!viewing) {
            player.openInventory(session.getInventory());
        }
    }

    private EconomyProvider economy(String name) {
        return plugin.getEconomyManager().get(name);
    }

    private void clearPendingFor(TradeSession session) {
        pendingInputs.entrySet().removeIf(en -> en.getValue().session() == session);
        session.setInputting(session.getPlayerA(), false);
        session.setInputting(session.getPlayerB(), false);
    }

    private String formatDouble(double amount) {
        return amount == Math.floor(amount)
                ? String.valueOf((long) amount)
                : String.format("%.2f", amount);
    }

    private boolean canAfford(TradeSession s, Player a, Player b) {
        EconomyProvider vault = economy("vault");

        if (s.getMoneyA() > 0 && (vault == null || vault.getBalance(a) < s.getMoneyA())) return false;
        if (s.getMoneyB() > 0 && (vault == null || vault.getBalance(b) < s.getMoneyB())) return false;

        // XP se mide en NIVELES ofrecidos
        if (s.getXpA() > 0 && a.getLevel() < s.getXpA()) return false;
        if (s.getXpB() > 0 && b.getLevel() < s.getXpB()) return false;

        return true;
    }

    private boolean exchangeCurrency(TradeSession s, Player a, Player b) {
        EconomyProvider vault = economy("vault");

        boolean tookA = false;

        if (s.getMoneyA() > 0) {
            if (vault == null || !vault.withdraw(a, s.getMoneyA())) return false;
            tookA = true;
        }

        if (s.getMoneyB() > 0) {
            if (vault == null || !vault.withdraw(b, s.getMoneyB())) {
                if (tookA) vault.deposit(a, s.getMoneyA());
                return false;
            }
        }

        if (s.getMoneyA() > 0) vault.deposit(b, s.getMoneyA());
        if (s.getMoneyB() > 0) vault.deposit(a, s.getMoneyB());

        // XP se transfiere por NIVELES (getTotalExperience no refleja los niveles reales en 1.18+)
        if (s.getXpA() > 0) {
            XpUtil.removeLevels(a, s.getXpA());
            XpUtil.giveLevels(b, s.getXpA());
        }
        if (s.getXpB() > 0) {
            XpUtil.removeLevels(b, s.getXpB());
            XpUtil.giveLevels(a, s.getXpB());
        }

        return true;
    }

    // =====================================================
    //                  CANCELAR TRADEO
    // =====================================================
    public void cancelTrade(TradeSession session, Player cancelledBy, String otherMsgKey) {
        if (session == null || session.isFinalizing()) return;

        session.cancelCountdown();
        session.setFinalizing(true);
        clearPendingFor(session);

        Inventory inv = session.getInventory();

        List<ItemStack> aItems = TradeMenu.readOffers(inv, TradeMenu.A_OFFER);
        List<ItemStack> bItems = TradeMenu.readOffers(inv, TradeMenu.B_OFFER);

        TradeMenu.clearOffers(inv, TradeMenu.A_OFFER);
        TradeMenu.clearOffers(inv, TradeMenu.B_OFFER);

        Player a = Bukkit.getPlayer(session.getPlayerA());
        Player b = Bukkit.getPlayer(session.getPlayerB());

        removeSession(session);

        // Devolver los ítems a sus dueños originales
        if (a != null && a.isOnline()) {
            closeIfTrade(a);
            giveItems(a, aItems);
        } else {
            Location fallback = b != null && b.isOnline() ? b.getLocation()
                    : (cancelledBy != null ? cancelledBy.getLocation() : null);
            dropItems(fallback, aItems);
        }

        if (b != null && b.isOnline()) {
            closeIfTrade(b);
            giveItems(b, bItems);
        } else {
            Location fallback = a != null && a.isOnline() ? a.getLocation()
                    : (cancelledBy != null ? cancelledBy.getLocation() : null);
            dropItems(fallback, bItems);
        }

        if (cancelledBy != null) {
            Lang.msg(cancelledBy, "trade.cancel");

            Player other = cancelledBy.getUniqueId().equals(session.getPlayerA()) ? b : a;
            if (other != null && otherMsgKey != null) {
                Lang.msg(other, otherMsgKey, "player", cancelledBy.getName());
            }
        }
    }

    public void cancelAll() {
        List<TradeSession> snap = new ArrayList<>(sessions.values().stream().distinct().toList());
        for (TradeSession session : snap) {
            cancelTrade(session, null, null);
        }
    }

    // =====================================================
    //                   HELPERS
    // =====================================================
    public TradeSession getSession(UUID uuid) {
        return sessions.get(uuid);
    }

    public boolean isInTrade(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    public boolean isInTrade(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    private void removeSession(TradeSession session) {
        if (sessions.get(session.getPlayerA()) == session) {
            sessions.remove(session.getPlayerA());
        }
        if (sessions.get(session.getPlayerB()) == session) {
            sessions.remove(session.getPlayerB());
        }
    }

    private void removeExpired() {
        long timeoutMs = plugin.getConfig().getInt("trade.request-timeout", 60) * 1000L;
        requests.entrySet().removeIf(en -> en.getValue().isExpired(timeoutMs));
    }

    private int maxDistance() {
        return plugin.getConfig().getInt("trade.max-distance", 10);
    }

    private boolean inRange(Player a, Player b) {
        if (a == null || b == null) return false;
        if (!a.getWorld().equals(b.getWorld())) return false;

        int dist = maxDistance();
        return a.getLocation().distanceSquared(b.getLocation()) <= dist * dist;
    }

    private boolean isViewing(Player player, TradeSession session) {
        return player.getOpenInventory().getTopInventory().getHolder() instanceof TradeHolder holder
                && holder.getSession() == session;
    }

    /** Simula la entrega sobre una copia del inventario. */
    private boolean fits(Player player, List<ItemStack> items) {
        Inventory copy = Bukkit.createInventory(null, 36);

        ItemStack[] storage = player.getInventory().getStorageContents();
        for (int i = 0; i < storage.length; i++) {
            if (storage[i] != null) copy.setItem(i, storage[i].clone());
        }

        for (ItemStack item : items) {
            if (!copy.addItem(item.clone()).isEmpty()) return false;
        }
        return true;
    }

    private void giveItems(Player p, List<ItemStack> items) {
        if (p == null || items == null) return;

        // Muerto: el inventario se vacia al reaparecer y los objetos se perderian
        if (p.isDead() || p.getHealth() <= 0) {
            dropItems(p.getLocation(), items);
            return;
        }

        for (ItemStack it : items) {
            if (it == null || it.getType().isAir()) continue;

            Map<Integer, ItemStack> left = p.getInventory().addItem(it);
            for (ItemStack rem : left.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), rem);
            }
        }
    }

    private void dropItems(Location location, List<ItemStack> items) {
        if (location == null || location.getWorld() == null || items == null) return;

        for (ItemStack it : items) {
            if (it != null && !it.getType().isAir()) {
                location.getWorld().dropItemNaturally(location, it);
            }
        }
    }

    private void closeIfTrade(Player p) {
        if (p == null || p.getOpenInventory() == null) return;
        if (p.getOpenInventory().getTopInventory().getHolder() instanceof TradeHolder) {
            p.closeInventory();
        }
    }
}