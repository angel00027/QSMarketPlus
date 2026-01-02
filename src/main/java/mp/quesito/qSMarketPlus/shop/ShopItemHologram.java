package mp.quesito.qSMarketPlus.shop;

import mp.quesito.qSMarketPlus.QSMarketPlus;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Arrays;

public class ShopItemHologram {

    private ItemDisplay display;
    private BukkitRunnable task;
    private final PlayerShop shop;
    private float angle = 0f;

    public ShopItemHologram(PlayerShop shop) {
        this.shop = shop;
    }

    public void spawn() {


        if (display != null && !display.isDead()) return; // ✅ evita duplicados

        var cfg = QSMarketPlus.getInstance().getConfig();

        if (!cfg.getBoolean("holograms.enabled")) return;

        remove();
        angle = 0f; // 🔧 RESET DE ROTACIÓN

        Inventory inv = shop.getChestInventory();
        if (inv == null) return;

        ItemStack item = Arrays.stream(inv.getContents())
                .filter(i -> i != null && i.getType() != Material.AIR)
                .findFirst()
                .orElse(null);

        if (item == null) return;

        // 📍 Offset desde config
        double x = cfg.getDouble("holograms.offset.x");
        double y = cfg.getDouble("holograms.offset.y");
        double z = cfg.getDouble("holograms.offset.z");

        Location loc = shop.getChestLocation().clone().add(x, y, z);

        display = loc.getWorld().spawn(loc, ItemDisplay.class);
        display.setItemStack(item.clone());
        display.setBillboard(Display.Billboard.CENTER);
        display.setViewRange(cfg.getInt("holograms.view-range"));

        // 📏 Escala
        float scale = (float) cfg.getDouble("holograms.scale");

        display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new Quaternionf(),
                new Vector3f(scale, scale, scale),
                new Quaternionf()
        ));

        // 🔄 Rotación configurable
        if (!cfg.getBoolean("holograms.rotation.enabled")) return;

        float speed = (float) cfg.getDouble("holograms.rotation.speed");
        int interval = cfg.getInt("holograms.rotation.interval");

        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (display == null || display.isDead()) {
                    cancel();
                    return;
                }

                angle += speed;
                if (angle >= 360) angle = 0;

                // 🌊 Flotación suave
                float floatOffset = (float) Math.sin(Math.toRadians(angle)) * 0.05f;

                display.setTransformation(new Transformation(
                        new Vector3f(0, floatOffset, 0),
                        new Quaternionf().rotateY((float) Math.toRadians(angle)),
                        new Vector3f(scale, scale, scale),
                        new Quaternionf()
                ));
            }
        };

        task.runTaskTimer(QSMarketPlus.getInstance(), 0L, interval);
    }

    public void remove() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        if (display != null && !display.isDead()) {
            display.remove();
            display = null;
        }
    }


}
