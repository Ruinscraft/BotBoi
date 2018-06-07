package com.ruinscraft.botboi.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.zaxxer.hikari.HikariDataSource;

public class MySqlStorage implements SqlStorage {

	private HikariDataSource dataSource;

	private String create_table;
	private String insert_key;
	private String insert_key_with_mojang_uuid;
	private String update_key;
	private String query_unverified;

	public MySqlStorage(String host, int port, String database, String username, String password, String botboiTable) {
		create_table = "CREATE TABLE IF NOT EXISTS " + botboiTable + " (token VARCHAR(36), discord_id VARCHAR(36), mojang_uuid VARCHAR(36), time BIGINT, UNIQUE (token, discord_id, mojang_uuid));";
		insert_key = "INSERT INTO " + botboiTable + " (token, discord_id, time) VALUES (?, ?, ?);";
		insert_key_with_mojang_uuid = "INSERT INTO " + botboiTable + " (token, discord_id, mojang_uuid, time) VALUES (?, ?, ?, ?);";
		update_key = "UPDATE" + botboiTable + " SET mojang_uuid = ?, discord_id = ? WHERE token = ?;";
		query_unverified = "SELECT discord_id FROM " + botboiTable + " WHERE mojang_uuid = NULL;";

		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (Exception e) {
			System.out.println("MySQL driver not found");
		}

		dataSource = new HikariDataSource();
		
		dataSource.setDriverClassName("com.mysql.jdbc.Driver");
		dataSource.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
		dataSource.setUsername(username);
		dataSource.setPassword(password);
		dataSource.setPoolName("botboi-pool");
		dataSource.setMaximumPoolSize(3);
		dataSource.setConnectionTimeout(3000);
		dataSource.setLeakDetectionThreshold(3000);

		try {
			createTable();
		} catch (SQLException e) {
			System.out.println("Could not connect to the database. Check your connection settings.");
		}
	}

	@Override
	public void insertKey(String key, String discordId, long time) {
		try (Connection c = getConnection();
				PreparedStatement insert = c.prepareStatement(insert_key)) {
			insert.setString(1, key);
			insert.setString(2, discordId);
			insert.setLong(3, time);
			insert.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void insertKey(String key, String discordId, UUID mojangUUID, long time) {
		try (Connection c = getConnection();
				PreparedStatement insert = c.prepareStatement(insert_key_with_mojang_uuid)) {
			insert.setString(1, key);
			insert.setString(2, discordId);
			insert.setString(3, mojangUUID.toString());
			insert.setLong(4, time);
			insert.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void updateKey(String key, String discordId, UUID mojangUUID) {
		try (Connection c = getConnection();
				PreparedStatement update = c.prepareStatement(update_key)) {
			update.setString(1, mojangUUID.toString());
			update.setString(2, discordId);
			update.setString(3, key);
			update.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Set<String> getUnverified() {
		Set<String> unverified = new HashSet<>();

		try (Connection c = getConnection();
				PreparedStatement query = c.prepareStatement(query_unverified)) {
			ResultSet rs = query.executeQuery();

			while (rs.next()) {
				unverified.add(rs.getString("discord_id"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return unverified;
	}

	@Override
	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	@Override
	public void createTable() throws SQLException {
		try (Connection c = getConnection();
				PreparedStatement update = c.prepareStatement(create_table)) {
			update.execute();
		} catch (SQLException e) {
			throw e;
		}
	}

}
