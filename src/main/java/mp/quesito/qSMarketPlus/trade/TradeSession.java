package mp.quesito.qSMarketPlus.trade;

import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TradeSession {

    private final UUID playerA;
    private final UUID playerB;

    private boolean readyA;
    private boolean readyB;
    private boolean finalizing;

    // Cuenta atrás antes de completar el tradeo (ambos están listos)
    private int countdown;
    private BukkitTask countdownTask;

    // Ofertas de moneda/XP (lo que ofrece cada jugador)
    private double moneyA;
    private double moneyB;
    private int xpA;
    private int xpB;

    private Inventory inventory;

    // Jugadores escribiendo la cantidad en el chat (su inventario puede estar cerrado)
    private final Set<UUID> inputting = new HashSet<>();

    public TradeSession(UUID playerA, UUID playerB) {
        this.playerA = playerA;
        this.playerB = playerB;
    }

    public boolean isPlayerA(UUID uuid) {
        return playerA.equals(uuid);
    }

    public boolean isPlayerB(UUID uuid) {
        return playerB.equals(uuid);
    }

    public boolean isParticipant(UUID uuid) {
        return playerA.equals(uuid) || playerB.equals(uuid);
    }

    public UUID getOther(UUID uuid) {
        return playerA.equals(uuid) ? playerB : playerA;
    }

    public UUID getPlayerA() {
        return playerA;
    }

    public UUID getPlayerB() {
        return playerB;
    }

    public boolean isReadyA() {
        return readyA;
    }

    public void setReadyA(boolean readyA) {
        this.readyA = readyA;
    }

    public boolean isReadyB() {
        return readyB;
    }

    public void setReadyB(boolean readyB) {
        this.readyB = readyB;
    }

    public boolean isReady(UUID uuid) {
        return isPlayerA(uuid) ? readyA : readyB;
    }

    public int getCountdown() {
        return countdown;
    }

    public void setCountdown(int countdown) {
        this.countdown = countdown;
    }

    public BukkitTask getCountdownTask() {
        return countdownTask;
    }

    public void setCountdownTask(BukkitTask countdownTask) {
        this.countdownTask = countdownTask;
    }

    public void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        countdown = 0;
    }

    public double getMoneyA() {
        return moneyA;
    }

    public void setMoneyA(double moneyA) {
        this.moneyA = moneyA;
    }

    public double getMoneyB() {
        return moneyB;
    }

    public void setMoneyB(double moneyB) {
        this.moneyB = moneyB;
    }

    public int getXpA() {
        return xpA;
    }

    public void setXpA(int xpA) {
        this.xpA = xpA;
    }

    public int getXpB() {
        return xpB;
    }

    public void setXpB(int xpB) {
        this.xpB = xpB;
    }

    public boolean isFinalizing() {
        return finalizing;
    }

    public void setFinalizing(boolean finalizing) {
        this.finalizing = finalizing;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public boolean isInputting(UUID uuid) {
        return inputting.contains(uuid);
    }

    public void setInputting(UUID uuid, boolean value) {
        if (value) {
            inputting.add(uuid);
        } else {
            inputting.remove(uuid);
        }
    }
}