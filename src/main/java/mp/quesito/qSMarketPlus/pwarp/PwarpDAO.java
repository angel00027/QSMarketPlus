package mp.quesito.qSMarketPlus.pwarp;

import mp.quesito.qSMarketPlus.database.SQLManager;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PwarpDAO {

    private final SQLManager sql;

    public PwarpDAO(SQLManager sql) {
        this.sql = sql;
    }

    // ==============================
    //   PWARPS
    // ==============================

    public void insert(Pwarp p) {
        sql.update("""
                INSERT INTO pwarp_warps
                (name, display_name, owner_uuid, owner_name, world, x, y, z, yaw, pitch, description, created, updated)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                p.getName(), p.getDisplayName(), p.getOwner().toString(), p.getOwnerName(),
                p.getWorldName(), p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch(),
                p.getDescription(), p.getCreated(), p.getUpdated());
    }

    public void update(Pwarp p) {
        sql.update("""
                UPDATE pwarp_warps
                SET display_name = ?, owner_name = ?, world = ?,
                    x = ?, y = ?, z = ?, yaw = ?, pitch = ?, description = ?, updated = ?
                WHERE name = ?
                """,
                p.getDisplayName(), p.getOwnerName(),
                p.getWorldName(), p.getX(), p.getY(), p.getZ(), p.getYaw(), p.getPitch(),
                p.getDescription(), p.getUpdated(),
                p.getName());
    }

    public void delete(String name) {
        sql.update("DELETE FROM pwarp_warps WHERE name = ?", name);
        sql.update("DELETE FROM pwarp_favorites WHERE warp_name = ?", name);
    }

    public List<Pwarp> loadAll() {
        List<Pwarp> list = new ArrayList<>();
        sql.query("SELECT * FROM pwarp_warps", rs -> {
            try {
                while (rs.next()) list.add(fromRow(rs));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        return list;
    }

    private Pwarp fromRow(ResultSet rs) throws Exception {
        String name = rs.getString("name");
        String displayName = rs.getString("display_name");
        UUID owner = UUID.fromString(rs.getString("owner_uuid"));
        String ownerName = rs.getString("owner_name");

        Pwarp p = new Pwarp(name, displayName, owner, ownerName);
        p.setLocation(
                rs.getString("world"),
                rs.getDouble("x"),
                rs.getDouble("y"),
                rs.getDouble("z"),
                rs.getFloat("yaw"),
                rs.getFloat("pitch")
        );
        String desc = rs.getString("description");
        p.setDescription(desc == null ? "" : desc);
        p.setCreated(rs.getLong("created"));
        p.setUpdated(rs.getLong("updated"));
        return p;
    }

    // ==============================
    //   FAVORITOS
    // ==============================

    public void addFavorite(UUID player, String warpName) {
        if (sql.isMySQL()) {
            sql.update("INSERT IGNORE INTO pwarp_favorites (player_uuid, warp_name) VALUES (?, ?)",
                    player.toString(), warpName);
        } else {
            sql.update("INSERT OR IGNORE INTO pwarp_favorites (player_uuid, warp_name) VALUES (?, ?)",
                    player.toString(), warpName);
        }
    }

    public void removeFavorite(UUID player, String warpName) {
        sql.update("DELETE FROM pwarp_favorites WHERE player_uuid = ? AND warp_name = ?",
                player.toString(), warpName);
    }

    public boolean isFavorite(UUID player, String warpName) {
        final boolean[] fav = {false};
        sql.query("SELECT warp_name FROM pwarp_favorites WHERE player_uuid = ? AND warp_name = ?", rs -> {
            try {
                fav[0] = rs.next();
            } catch (Exception ignored) {
            }
        }, player.toString(), warpName);
        return fav[0];
    }

    public List<String> loadFavorites(UUID player) {
        List<String> list = new ArrayList<>();
        sql.query("SELECT warp_name FROM pwarp_favorites WHERE player_uuid = ?", rs -> {
            try {
                while (rs.next()) list.add(rs.getString("warp_name"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, player.toString());
        return list;
    }
}