package mp.quesito.qSMarketPlus.pwarp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public class Pwarp {

    private final String name;
    private final UUID owner;
    private String ownerName;
    private String displayName;
    private String world;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private String description;
    private long created;
    private long updated;

    public Pwarp(String name, String displayName, UUID owner, String ownerName) {
        this.name = name;
        this.displayName = displayName;
        this.owner = owner;
        this.ownerName = ownerName;
        this.description = "";
        long now = System.currentTimeMillis();
        this.created = now;
        this.updated = now;
    }

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public UUID getOwner() { return owner; }
    public String getOwnerName() { return ownerName; }
    public String getDescription() { return description; }
    public String getWorldName() { return world; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public long getCreated() { return created; }
    public long getUpdated() { return updated; }

    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public void setDescription(String description) { this.description = description; }
    public void setCreated(long created) { this.created = created; }
    public void setUpdated(long updated) { this.updated = updated; }

    public Location getLocation() {
        World w = Bukkit.getWorld(world);
        if (w == null) return null;
        return new Location(w, x, y, z, yaw, pitch);
    }

    public void setLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
        this.world = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void setLocation(Location loc) {
        this.world = loc.getWorld().getName();
        this.x = loc.getX();
        this.y = loc.getY();
        this.z = loc.getZ();
        this.yaw = loc.getYaw();
        this.pitch = loc.getPitch();
    }
}