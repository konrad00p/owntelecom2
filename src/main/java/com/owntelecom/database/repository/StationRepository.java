package com.owntelecom.database.repository;

import com.owntelecom.database.DatabaseManager;
import com.owntelecom.database.model.Station;

import java.sql.*;
import java.util.*;

public class StationRepository {

    private final DatabaseManager db;

    public StationRepository(DatabaseManager db) {
        this.db = db;
    }

    public List<Station> findByOperator(String operatorId) {
        try {
            return db.runSync(conn -> {
                List<Station> list = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM stations WHERE operator_id = ?")) {
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

    public List<Station> findByWorld(String world) {
        try {
            return db.runSync(conn -> {
                List<Station> list = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM stations WHERE world = ? AND broken = 0")) {
                    ps.setString(1, world);
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

    public Optional<Station> findById(int id) {
        try {
            return db.runSync(conn -> {
                try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM stations WHERE id = ?")) {
                    ps.setInt(1, id);
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

    public int create(Station station) {
        try {
            return db.runSync(conn -> {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO stations (operator_id, world, x, y, z, technology, level, broken, last_maintenance) VALUES (?,?,?,?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, station.operatorId().toLowerCase(Locale.ROOT));
                    ps.setString(2, station.world());
                    ps.setInt(3, station.x());
                    ps.setInt(4, station.y());
                    ps.setInt(5, station.z());
                    ps.setString(6, station.technology());
                    ps.setInt(7, station.level());
                    ps.setInt(8, station.broken() ? 1 : 0);
                    ps.setLong(9, station.lastMaintenance());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            return keys.getInt(1);
                        }
                    }
                }
                return -1;
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setBroken(int id, boolean broken) {
        try {
            db.runSync(conn -> {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE stations SET broken = ? WHERE id = ?")) {
                    ps.setInt(1, broken ? 1 : 0);
                    ps.setInt(2, id);
                    ps.executeUpdate();
                }
                return null;
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setLevel(int id, int level) {
        try {
            db.runSync(conn -> {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE stations SET level = ? WHERE id = ?")) {
                    ps.setInt(1, level);
                    ps.setInt(2, id);
                    ps.executeUpdate();
                }
                return null;
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(int id) {
        try {
            db.runSync(conn -> {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM stations WHERE id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                return null;
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteByOperator(String operatorId) {
        try {
            db.runSync(conn -> {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM stations WHERE operator_id = ?")) {
                    ps.setString(1, operatorId.toLowerCase(Locale.ROOT));
                    ps.executeUpdate();
                }
                return null;
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Station map(ResultSet rs) throws SQLException {
        return new Station(
                rs.getInt("id"),
                rs.getString("operator_id"),
                rs.getString("world"),
                rs.getInt("x"),
                rs.getInt("y"),
                rs.getInt("z"),
                rs.getString("technology"),
                rs.getInt("level"),
                rs.getInt("broken") == 1,
                rs.getLong("last_maintenance")
        );
    }
}
