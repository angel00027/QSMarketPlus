package mp.quesito.qSMarketPlus.auction;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import mp.quesito.qSMarketPlus.auction.menu.AHMenu;
import mp.quesito.qSMarketPlus.utils.Lang;
import mp.quesito.qSMarketPlus.utils.MessageUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;
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

    // Historial de un jugador
    public List<AuctionItem> getHistory(UUID seller) {
        return dao.loadHistory(seller, 100, 0);
    }

    // ==========================================
    // CREAR SUBASTA NORMAL (Mano / Individual)
    // ==========================================
// ==========================================
    // CREAR SUBASTA NORMAL (Mano / Individual)
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

        dao.insertAuction(auction);
        activeAuctions.add(auction);

        String itemName = item.getItemMeta().hasDisplayName()
                ? MessageUtil.toLegacy(item.getItemMeta().getDisplayName())
                : item.getType().name().replace("_", " ").toLowerCase();

        // Enviar anuncio formal estructurado pasando parámetros en formato Clave -> Valor tradicional
        for (Player online : Bukkit.getOnlinePlayers()) {
            Lang.msg(online, "ah_broadcast_header");
            Lang.msg(online, "ah_broadcast_title");

            Lang.msg(online, "ah_broadcast_player", "player", seller.getName());
            Lang.msg(online, "ah_broadcast_price", "price", String.valueOf(price));
            Lang.msg(online, "ah_broadcast_type", "type", "Individual");
            Lang.msg(online, "ah_broadcast_item", "item", itemName);

            Lang.msg(online, "ah_broadcast_footer");
        }

        refreshAllActiveMenus();
    }

    // ==========================================
    // CREAR SUBASTA BULK (Inventario / Paquete)
    // ==========================================
    public void createBulkAuction(Player seller, ItemStack[] items, double price) {
        long now = System.currentTimeMillis();
        long seconds = plugin.getConfig().getLong("auction.expiration_seconds", 86400);
        long durationMs = seconds * 1000L;
        long expires = now + durationMs;

        ItemStack[] clean = Arrays.stream(items)
                .map(i -> i == null ? null : i.clone())
                .toArray(ItemStack[]::new);

        for (ItemStack req : items) {
            if (req == null || req.getType().isAir()) continue;

            int needed = req.getAmount();
            ItemStack[] contents = seller.getInventory().getContents();

            for (int i = 0; i < contents.length && needed > 0; i++) {
                ItemStack slot = contents[i];
                if (slot == null) continue;
                if (!slot.isSimilar(req)) continue;

                int take = Math.min(slot.getAmount(), needed);

                if (take == slot.getAmount()) contents[i] = null;
                else slot.setAmount(slot.getAmount() - take);

                needed -= take;
            }
            seller.getInventory().setContents(contents);
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

        long totalCount = Arrays.stream(clean)
                .filter(Objects::nonNull)
                .mapToLong(ItemStack::getAmount)
                .sum();

        // Enviar anuncio formal estructurado pasando parámetros en formato Clave -> Valor tradicional
        for (Player online : Bukkit.getOnlinePlayers()) {
            Lang.msg(online, "ah_broadcast_header");
            Lang.msg(online, "ah_broadcast_title");

            Lang.msg(online, "ah_broadcast_player", "player", seller.getName());
            Lang.msg(online, "ah_broadcast_price", "price", String.valueOf(price));
            Lang.msg(online, "ah_broadcast_type", "type", "Masiva");
            Lang.msg(online, "ah_broadcast_item", "item", "Lote x" + totalCount);

            Lang.msg(online, "ah_broadcast_footer");
        }

        refreshAllActiveMenus();
    }

    // ==========================================
    // COMPRAR SUBASTA
    // ==========================================
    public boolean buyAuction(Player buyer, AuctionItem auction) {
        if (!activeAuctions.contains(auction)) return false;

        auction.status = AuctionItem.Status.SOLD;
        auction.buyer = buyer.getUniqueId();

        activeAuctions.remove(auction);
        dao.updateStatusAndBuyer(auction);

        // Notificar y limpiar pantallas de inmediato
        refreshAllActiveMenus();
        return true;
    }

    // ==========================================
    // CANCELAR SUBASTA
    // ==========================================
    public boolean cancelAuction(Player player, AuctionItem auction) {
        if (!auction.seller.equals(player.getUniqueId()))
            return false;

        auction.status = AuctionItem.Status.EXPIRED;
        dao.updateStatus(auction);

        activeAuctions.remove(auction);

        Lang.msg(player, "auction-cancelled-title");
        Lang.msg(player, "auction-cancelled-footer");

        refreshAllActiveMenus();
        return true;
    }

    // ==========================================
    // 🔥 MOTOR DE SINCRONIZACIÓN SÍNCRONA (REALTIME)
    // ==========================================
    public void refreshAllActiveMenus() {
        // Ejecutamos en el siguiente tick del hilo principal del servidor para prevenir excepciones de concurrencia
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getOpenInventory() != null) {
                    InventoryView openInv = online.getOpenInventory();
                    if (openInv.getTopInventory() != null &&
                            openInv.getTopInventory().getHolder() instanceof mp.quesito.qSMarketPlus.auction.holder.AHHolder holder) {

                        // Re-abrir la página actual fuerza a recalcular los filtros y el grid gráfico
                        AHMenu.open(online, holder.getPage());
                    }
                }
            }
        });
    }

    public List<AuctionItem> getExpired(UUID seller) {
        return dao.loadExpired(seller);
    }

    public void markAsTaken(AuctionItem auc) {
        dao.markAsTaken(auc);
    }

    public AuctionDAO getDao() {
        return dao;
    }

    public List<AuctionItem> getMutableList() {
        return activeAuctions;
    }

    public int getAuctionLimit(Player p) {
        // 1. Límite base por defecto (puedes dejarlo fijo o leer un único valor de la config)
        int limit = plugin.getConfig().getInt("auction-limits.default", 5);

        // 2. Definimos el prefijo que vamos a buscar
        String prefix = "qsmarket.ah.limit.";

        // 3. Revisamos todos los permisos efectivos que tiene asignados el jugador
        for (org.bukkit.permissions.PermissionAttachmentInfo attachment : p.getEffectivePermissions()) {
            String perm = attachment.getPermission();

            // Si el permiso empieza con nuestro prefijo y está activo (true)
            if (perm.startsWith(prefix) && attachment.getValue()) {
                // Extraemos la parte numérica (lo que va después de "qsmarket.limit.")
                String limitStr = perm.substring(prefix.length());

                try {
                    int foundLimit = Integer.parseInt(limitStr);
                    // Nos quedamos con el valor más alto que tenga el jugador
                    limit = Math.max(limit, foundLimit);
                } catch (NumberFormatException e) {
                    // Ignora el permiso si el final no es un número válido (ej: qsmarket.limit.vip)
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