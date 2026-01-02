package mp.quesito.qSMarketPlus.shop;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public final class ShopCategory {

    // ===================== DATOS BASE =====================
    private final String id;
    private final String name;
    private final Material material;
    private final int slot;
    private final List<String> lore;
    private final String headTexture;

    // ===================== RESTRICCIONES =====================
    private final String requiredPermission;
    private final String requiredGroup;

    // ===================== CONSTRUCTOR =====================
    public ShopCategory(
            String id,
            String name,
            Material material,
            int slot,
            List<String> lore,
            String headTexture,
            String requiredPermission,
            String requiredGroup
    ) {
        this.id = id;
        this.name = name;
        this.material = material != null ? material : Material.CHEST;
        this.slot = slot;
        this.lore = lore != null ? List.copyOf(lore) : List.of();
        this.headTexture = headTexture;

        this.requiredPermission = requiredPermission;
        this.requiredGroup = requiredGroup;
    }

    // ===================== GETTERS =====================
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Material getMaterial() {
        return material;
    }

    public int getSlot() {
        return slot;
    }

    public List<String> getLore() {
        return lore;
    }

    public String getHeadTexture() {
        return headTexture;
    }

    public String getRequiredPermission() {
        return requiredPermission;
    }

    public String getRequiredGroup() {
        return requiredGroup;
    }



    // ===================== PERMISOS =====================
    public boolean canAccess(Player player) {

        if (requiredPermission != null && !requiredPermission.isEmpty()) {
            if (!player.hasPermission(requiredPermission)) return false;
        }

        if (requiredGroup != null && !requiredGroup.isEmpty()) {
            if (!player.hasPermission("group." + requiredGroup)) return false;
        }

        return true;
    }

}
