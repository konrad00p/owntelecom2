package com.owntelecom.database.repository;

import com.owntelecom.database.DatabaseManager;
import com.owntelecom.database.model.Zone;
import com.owntelecom.database.model.ZoneType;

import java.sql.*;
import java.util.*;

public class ZoneRepository {

    private final DatabaseManager db;

    public ZoneRepository(DatabaseManager db) {
        this.db = db;
    }

    public Optional<Zone> findById(String id) {
        try {
            return db.runSync(conn -> {
                try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM zones WHERE id = ?")) {
                    ps.setString(1, id.toLowerCase(Locale.ROOT));
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return Optional.of(map(rs));
                        }
                    }
                }
                return Optional.empty();
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Zone> findByOperator(String operatorId) {
        try {
            return db.runSync(conn -> {
                List<Zone> list = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM zones WHERE operator_id = ?")) {
                    ps.setString(1, operatorId.toLowerCase(Locale.ROOT));
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            list.add(map(rs));
                        }
                    }
                }
                return list;
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Zone> findByOperatorAndType(String operatorId, ZoneType type) {
        try {
            return db.runSync(conn -> {
                List<Zone> list = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM zones WHERE operator_id = ? AND type = ?")) {
                    ps.setString(1, operatorId.toLowerCase(Locale.ROOT));
                    ps.setString(2, type.name());
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            list.add(map(rs));
                        }
                    }
                }
                return list;
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Zone> findZoneForOperator(String operatorId, String targetOperatorId, ZoneType type) {
        try {
            return db.runSync(conn -> {
                try (PreparedStatement ps = conn.prepareStatement("""
                        SELECT z.* FROM zones z
                        INNER JOIN zone_members zm ON z.id = zm.zone_id
                        WHERE z.operator_id = ? AND zm.target_operator_id = ? AND z.type = ?
                        """)) {
                    ps.setString(1, operatorId.toLowerCase(Locale.ROOT));
                    ps.setString(2, targetOperatorId.toLowerCase(Locale.ROOT));
                    ps.setString(3, type.name());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return Optional.of(map(rs));
                        }
                    }
                }
                return Optional.empty();
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void create(Zone zone) {
        try {
            db.runSync(conn -> {
                try (PreparedStatement ps = conn.prepareStatement("""
                        INSERT INTO zones (id, operator_id, type, display_name, extra_minute, extra_sms, extra_mb)
                        VALUES (?,?,?,?,?,?,?)
                        """)) {
                    ps.setString(1, zone.id().toLowerCase(Locale.ROOT));
                    ps.setString(2, zone.operatorId().toLowerCase(Locale.ROOT));
                    ps.setString(3, zone.type().name());
                    ps.setString(4, zone.displayName());
                    ps.setDouble(5, zone.extraMinute());
                    ps.setDouble(6, zone.extraSms());
                    ps.setDouble(7, zone.extraMb());
                    ps.executeUpdate();
                }
                return null;
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void update(Zone zone) {
        try {
            db.runSync(conn -> {
                try (PreparedStatement ps = conn.prepareStatement("""
                        UPDATE zones SET display_name = ?, extra_minute = ?, extra_sms = ?, extra_mb = ?
                        WHERE id = ?
                        """)) {
                    ps.setString(1, zone.displayName());
                    ps.setDouble(2, zone.extraMinute());
                    ps.setDouble(3, zone.extraSms());
                    ps.setDouble(4, zone.extraMb());
                    ps.setString(5, zone.id().toLowerCase(Locale.ROOT));
                    ps.executeUpdate();
                }
                return null;
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(String id) {
        try {
            db.runSync(conn -> {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM zones WHERE id = ?")) {
                    ps.setString(1, id.toLowerCase(Locale.ROOT));
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM zone_members WHERE zone_id = ?")) {
                    ps.setString(1, id.toLowerCase(Locale.ROOT));
                    ps.executeUpdate();
                }
                return null;
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void addMember(String zoneId, String targetOperatorId) {
        try {
            db.runSync(conn -> {
                try (PreparedStatement ps = conn.prepareStatement("""
                        INSERT OR IGNORE INTO zone_members (zone_id, target_operator_id)
                        VALUES (?,?)
                        """)) {
                    ps.setString(1, zoneId.toLowerCase(Locale.ROOT));
                    ps.setString(2, targetOperatorId.toLowerCase(Locale.ROOT));
                    ps.executeUpdate();
                }
                return null;
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeMember(String zoneId, String targetOperatorId) {
        try {
            db.runSync(conn -> {
                try (PreparedStatement ps = conn.prepareStatement("""
                        DELETE FROM zone_members WHERE zone_id = ? AND target_operator_id = ?
                        """)) {
                    ps.setString(1, zoneId.toLowerCase(Locale.ROOT));
                    ps.setString(2, targetOperatorId.toLowerCase(Locale.ROOT));
                    ps.executeUpdate();
                }
                return null;
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getZoneMembers(String zoneId) {
        try {
            return db.runSync(conn -> {
                List<String> list = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement("SELECT target_operator_id FROM zone_members WHERE zone_id = ?")) {
                    ps.setString(1, zoneId.toLowerCase(Locale.ROOT));
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            list.add(rs.getString("target_operator_id"));
                        }
                    }
                }
                return list;
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Zone map(ResultSet rs) throws SQLException {
        return new Zone(
                rs.getString("id"),
                rs.getString("operator_id"),
                ZoneType.valueOf(rs.getString("type")),
                rs.getString("display_name"),
                rs.getDouble("extra_minute"),
                rs.getDouble("extra_sms"),
                rs.getDouble("extra_mb")
        );
    }
}
