package mp.quesito.qSMarketPlus.auction;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class AuctionManager {

    private final QSMarketPlus plugin;
    private final AuctionDAO dao;

    // Subastas activas en memoria
    private final List<AuctionItem> activeAuctions = new ArrayList<>();

    public AuctionManager(QSMarketPlus plugin) {
        this.plugin = plugin;
        this.dao = new AuctionDAO(plugin.getSqlManager());

        loadActiveFromSQL();
        // Tarea de expiración automática
        long interval = plugin.getConfig().getLong("auction.expiration_check_interval", 300);
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin,
                new AuctionExpirationTask(this),
                20L * interval,
                20L * interval
        );

    }

    private void loadActiveFromSQL() {
        activeAuctions.clear();
        activeAuctions.addAll(dao.loadActiveAuctions());
        plugin.getLogger().info("Cargadas " + activeAuctions.size() + " subastas activas desde SQL.");
    }

    // Solo lectura de activas
    public List<AuctionItem> getAuctions() {
        return Collections.unmodifiableList(activeAuctions);
    }

    // Historial de un jugador (puedes paginar a gusto)
    public List<AuctionItem> getHistory(UUID seller) {
        // 100 por defecto, 0 offset. Si quieres paginación real, expón limit/offset también.
        return dao.loadHistory(seller, 100, 0);
    }

    // ==========================================
    // CREAR SUBASTA NORMAL
    // ==========================================
    public void createAuction(Player seller, ItemStack item, double price) {

        long now = System.currentTimeMillis();

        long seconds = plugin.getConfig().getLong("auction.expiration_seconds", 86400);
        long durationMs = seconds * 1000L;

        long expires = now + durationMs;

        AuctionItem auction = new AuctionItem(
                seller.getUniqueId(),
                item.clone(),
                price,
                now,
                expires
        );

        auction.status = AuctionItem.Status.ACTIVE;

        // Guardar en SQL — PERO NO QUITAR NADA DEL INVENTARIO
        dao.insertAuction(auction);

        activeAuctions.add(auction);
    }


    // ==========================================
    // CREAR SUBASTA BULK
    // ==========================================
    public void createBulkAuction(Player seller, ItemStack[] items, double price) {

        long now = System.currentTimeMillis();

        long seconds = plugin.getConfig().getLong("auction.expiration_seconds", 86400);
        long durationMs = seconds * 1000L;

        long expires = now + durationMs;

        // Clonar antes de eliminar
        ItemStack[] clean = Arrays.stream(items)
                .map(i -> i == null ? null : i.clone())
                .toArray(ItemStack[]::new);

        // 🔥 Eliminar EXACTAMENTE los items del inventario
        for (ItemStack req : items) {

            if (req == null || req.getType().isAir()) continue;

            int needed = req.getAmount();

            ItemStack[] contents = seller.getInventory().getContents();

            for (int i = 0; i < contents.length && needed > 0; i++) {

                ItemStack slot = contents[i];
                if (slot == null) continue;

                if (!slot.isSimilar(req)) continue;

                int take = Math.min(slot.getAmount(), needed);

                if (take == slot.getAmount()) contents[i] = null; // slot vacío
                else slot.setAmount(slot.getAmount() - take);

                needed -= take;
            }
        }

        AuctionItem auction = new AuctionItem(
                seller.getUniqueId(),
                clean,
                price,
                now,
                expires
        );

        auction.status = AuctionItem.Status.ACTIVE;

        dao.insertAuction(auction);
        activeAuctions.add(auction);
    }


    // ==========================================
    // COMPRAR SUBASTA (solo estado / memoria)
    // Importante: ya NO maneja dinero ni items.
    // Eso lo haces tú en el menú antes de llamar.
    // ==========================================
    public boolean buyAuction(Player buyer, AuctionItem auction) {

        if (!activeAuctions.contains(auction)) return false;

        auction.status = AuctionItem.Status.SOLD;
        auction.buyer = buyer.getUniqueId();

        activeAuctions.remove(auction);
        dao.updateStatusAndBuyer(auction);

        return true;
    }

    public List<AuctionItem> getExpired(UUID seller) {
        return dao.loadExpired(seller);
    }

    public void markAsTaken(AuctionItem auc) {
        dao.markAsTaken(auc);
    }


    // ==========================================
    // CANCELAR SUBASTA
    // Devuelve ítems + actualiza estado
    // ==========================================
    public boolean cancelAuction(Player player, AuctionItem auction) {

        if (!auction.seller.equals(player.getUniqueId()))
            return false;

        // 🔥 SIEMPRE ENVIAR A EXPIRADOS (Nunca devolver items aquí)
        auction.status = AuctionItem.Status.EXPIRED;
        dao.updateStatus(auction);

        activeAuctions.remove(auction);

        player.sendMessage("§eTu subasta ha sido cancelada.");
        player.sendMessage("§7Los ítems fueron enviados a §6/AH Expirados §7para reclamarlos después.");

        return true;
    }


    public AuctionDAO getDao() {
        return dao;
    }

    public List<AuctionItem> getMutableList() {
        return activeAuctions;
    }

    public int getAuctionLimit(Player p) {
        FileConfiguration cfg = plugin.getConfig();

        int limit = cfg.getInt("auction-limits.default", 5);

        if (cfg.isConfigurationSection("auction-limits")) {
            for (String perm : cfg.getConfigurationSection("auction-limits").getKeys(false)) {
                if (perm.equals("default")) continue;

                if (p.hasPermission(perm)) {
                    limit = Math.max(limit, cfg.getInt("auction-limits." + perm));
                }
            }
        }

        return limit;
    }
    public int getActiveAuctions(Player p) {
        int count = 0;

        for (AuctionItem auc : activeAuctions) {
            if (!auc.isExpired() && auc.seller.equals(p.getUniqueId())) {
                count++;
            }
        }

        return count;
    }



}
