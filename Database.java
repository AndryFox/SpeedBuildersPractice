package it.andrea.speedbuilders;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class Database {
    private Connection connection;
    private final String host;
    private final String database;
    private final String user;
    private final String password;
    private final int port;

    public Database(String host, int port, String database, String user, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
    }

    public boolean connect() {
        try {
            if (connection != null && !connection.isClosed()) return true;
            synchronized (this) {
                if (connection != null && !connection.isClosed()) return true;
                Class.forName("org.postgresql.Driver");
                // URL format standard per PostgreSQL / Supabase
                String url = "jdbc:postgresql://" + host + ":" + port + "/" + database + "?sslmode=require";
                connection = DriverManager.getConnection(url, user, password);
                return true;
            }
        } catch (Exception e) {
            System.err.println("[SpeedBuilders] Errore di connessione a Supabase: " + e.getMessage());
            return false;
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Estrae la Top 10 per una singola mappa simulando la logica del bot
    public Map<String, Float> getTopRecords(String buildName, int limit) {
        Map<String, Float> top = new LinkedHashMap<>();
        if (!connect()) return top;

        try {
            String query = "SELECT player_name, time FROM WorldRecords WHERE LOWER(build_name) = LOWER(?) ORDER BY time ASC LIMIT ?";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, buildName);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                top.put(rs.getString("player_name"), rs.getFloat("time"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return top;
    }

    // Calcola la Top 10 globale unendo gli alias
    public java.util.LinkedHashMap<String, Integer> getTopWRHolders(int limit) {
        java.util.LinkedHashMap<String, Integer> top = new java.util.LinkedHashMap<>();
        if (!connect()) return top;

        try {
            String query = "SELECT COALESCE(MAX(a.new_name), MAX(r1.player_name)) AS display_name, " +
                    "COUNT(*) as wr_count FROM WorldRecords r1 " +
                    "LEFT JOIN Aliases a ON LOWER(r1.player_name) = a.old_name " +
                    "WHERE time = (SELECT MIN(time) FROM WorldRecords r2 WHERE LOWER(r1.build_name) = LOWER(r2.build_name)) " +
                    "GROUP BY LOWER(COALESCE(a.new_name, r1.player_name)) " +
                    "ORDER BY wr_count DESC LIMIT ?";

            PreparedStatement ps = connection.prepareStatement(query);
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                top.put(rs.getString("display_name"), rs.getInt("wr_count"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return top;
    }

    // Calcola il totale dei WR del singolo giocatore risolvendo gli alias
    public int getPlayerWRCount(String playerName) {
        if (!connect()) return 0;

        try {
            String query = "SELECT COUNT(*) AS wr_count FROM WorldRecords r1 " +
                    "LEFT JOIN Aliases a ON LOWER(r1.player_name) = a.old_name " +
                    "WHERE LOWER(COALESCE(a.new_name, r1.player_name)) = LOWER(?) " +
                    "AND time = (SELECT MIN(time) FROM WorldRecords r2 WHERE LOWER(r1.build_name) = LOWER(r2.build_name))";

            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, playerName);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("wr_count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

}