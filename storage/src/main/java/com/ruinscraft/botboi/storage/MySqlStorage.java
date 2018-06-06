package com.ruinscraft.botboi.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import com.zaxxer.hikari.HikariDataSource;

public class MySqlStorage implements SqlStorage {

	private HikariDataSource dataSource;

	private String create_table;
	private String insert_key;
	private String insert_key_with_mojang_uuid;
	private String query_linked_account;

	public MySqlStorage(String host, int port, String database, String username, String password, String botboiTable) {
		dataSource = new HikariDataSource();

		dataSource.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
		dataSource.setUsername(username);
		dataSource.setPassword(password);
		dataSource.setPoolName("botboi-pool");
		dataSource.setMaximumPoolSize(3);
		dataSource.setConnectionTimeout(3000);
		dataSource.setLeakDetectionThreshold(3000);

		createTable();
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
	public UUID getLinkedAccount(String discordId) {
		UUID uuid = null;

		try (Connection c = getConnection();
				PreparedStatement query = c.prepareStatement(query_linked_account)) {
			query.setString(1, discordId);
			
			ResultSet rs = query.executeQuery();

			while (rs.next()) {
				uuid = UUID.fromString(rs.getString("mojang_uuid"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return uuid;
	}

	@Override
	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	@Override
	public void createTable() {
		try (Connection c = getConnection();
				PreparedStatement update = c.prepareStatement(create_table)) {
			update.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
