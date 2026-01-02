package mp.quesito.qSMarketPlus.auction;

import mp.quesito.qSMarketPlus.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Iterator;

public class AuctionExpirationTask implements Runnable {

    private final AuctionManager manager;

    public AuctionExpirationTask(AuctionManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {

        long now = System.currentTimeMillis();

        Iterator<AuctionItem> it = manager.getMutableList().iterator();

        while (it.hasNext()) {

            AuctionItem auc = it.next();

            if (auc.status != AuctionItem.Status.ACTIVE) continue;
            if (auc.expiresAt > now) continue;

            // ============================
            //  NOTIFICAR AL VENDEDOR
            // ============================
            Player seller = Bukkit.getPlayer(auc.seller);
            if (seller != null && seller.isOnline()) {

                Lang.msg(seller, "ah_expired",
                        "item", auc.getReadableName()
                );
            }
            // Avisar al vendedor
            String readable = auc.getReadableName();

            if (seller != null && seller.isOnline()) {
                Lang.msg(seller, "ah_expired", "item", readable);
            }

            // Enviar webhook
            Lang.discord("❗ La subasta de **" + readable + "** del jugador **"
                    + (seller != null ? seller.getName() : "Desconocido")
                    + "** ha expirado.");
            // ============================
            //  MARCAR COMO EXPIRADO
            // ============================
            auc.status = AuctionItem.Status.EXPIRED;
            manager.getDao().updateStatus(auc);

            it.remove();
        }
    }
}
