package com.owntelecom.database.repository;

import com.owntelecom.database.DatabaseManager;
import com.owntelecom.database.model.Datacenter;

import java.sql.*;
import java.util.*;

public class DatacenterRepository {

    private final DatabaseManager db;

    public DatacenterRepository(DatabaseManager db) {
        this.db = db;
    }

    public Optional<Datacenter> findById(int id) {
        return db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM datacenters WHERE id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(map(rs));
                    }
                }
            }
            return Optional.empty();
        });
    }

    public List<Datacenter> findByOwner(UUID ownerUuid) {
        return db.runSync(conn -> {
            List<Datacenter> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM datacenters WHERE owner_uuid = ?")) {
                ps.setString(1, ownerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(map(rs));
                    }
                }
            }
            return list;
        });
    }

    public Optional<Datacenter> findAtLocation(String world, int x, int y, int z) {
        return db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM datacenters WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
                ps.setString(1, world);
                ps.setInt(2, x);
                ps.setInt(3, y);
                ps.setInt(4, z);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(map(rs));
                    }
                }
            }
            return Optional.empty();
        });
    }

    public int create(Datacenter datacenter) {
        return db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO datacenters (owner_uuid, world, x, y, z, level, broken)
                    VALUES (?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, datacenter.ownerUuid().toString());
                ps.setString(2, datacenter.world());
                ps.setInt(3, datacenter.x());
                ps.setInt(4, datacenter.y());
                ps.setInt(5, datacenter.z());
                ps.setInt(6, datacenter.level());
                ps.setInt(7, datacenter.broken() ? 1 : 0);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
            return -1;
        });
    }

    public void updateLevel(int id, int level) {
        db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE datacenters SET level = ? WHERE id = ?")) {
                ps.setInt(1, level);
                ps.setInt(2, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public void setBroken(int id, boolean broken) {
        db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE datacenters SET broken = ? WHERE id = ?")) {
                ps.setInt(1, broken ? 1 : 0);
                ps.setInt(2, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public void delete(int id) {
        db.runSync(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM datacenters WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    private Datacenter map(ResultSet rs) throws SQLException {
        return new Datacenter(
                rs.getInt("id"),
                UUID.fromString(rs.getString("owner_uuid")),
                rs.getString("world"),
                rs.getInt("x"),
                rs.getInt("y"),
                rs.getInt("z"),
                rs.getInt("level"),
                rs.getInt("broken") == 1
        );
    }
}
