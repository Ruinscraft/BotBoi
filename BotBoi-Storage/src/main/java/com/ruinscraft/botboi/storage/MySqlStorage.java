package com.ruinscraft.botboi.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.zaxxer.hikari.HikariDataSource;

public class MySqlStorage implements SqlStorage {

	private HikariDataSource dataSource;
	private HikariDataSource luckPermsDataSource;

	private String create_table;
	private String insert_token;
	private String update_token_set_waiting;
	private String update_token_set_used;
	private String update_token_set_uuid;
	private String query_waiting;
	private String query_token;
	private String query_ids_and_uuids;
	private String delete_user;

	private String luckperms_query_username;
	private String luckperms_query_permission;
	private String luckperms_query_group_permission;
	private String luckperms_query_group_permission_list;

	public MySqlStorage(String host, int port, String database, String username, String password, 
			String botboiTable, String luckPermsDatabase, String luckPermsPlayerTable, 
			String luckPermsPermTable, String luckPermsGroupPermTable) {
		create_table = "CREATE TABLE IF NOT EXISTS " + botboiTable + " (token VARCHAR(36), discord_id VARCHAR(36), uuid VARCHAR(36), waiting BOOL DEFAULT 0, used BOOL DEFAULT 0, UNIQUE (token));";
		insert_token = "INSERT INTO " + botboiTable + " (token, discord_id) VALUES (?, ?);";
		update_token_set_waiting = "UPDATE " + botboiTable + " SET waiting = ? WHERE token = ?;";
		update_token_set_used = "UPDATE " + botboiTable + " SET used = ? WHERE token = ?;";
		update_token_set_uuid = "UPDATE " + botboiTable + " SET uuid = ? WHERE token = ?;";
		query_waiting = "SELECT token, discord_id, uuid FROM " + botboiTable + " WHERE waiting = 1 AND uuid IS NOT NULL;";
		query_token = "SELECT * FROM " + botboiTable + " WHERE token = ?;";
		query_ids_and_uuids = "SELECT discord_id, uuid FROM " + botboiTable + " WHERE uuid IS NOT NULL;";
		delete_user = "DELETE FROM " + botboiTable + " WHERE discord_id = ?;";

		luckperms_query_username = "SELECT username FROM " + luckPermsPlayerTable + " WHERE uuid = ?;";
		luckperms_query_permission = "SELECT value FROM " + luckPermsPermTable + " WHERE uuid = ? AND permission = ?;";
		luckperms_query_group_permission = "SELECT value FROM " + luckPermsGroupPermTable + " WHERE name = ? AND permission = ?;";
		luckperms_query_group_permission_list = "SELECT permission FROM " + luckPermsGroupPermTable + " WHERE name = ?;";

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

		luckPermsDataSource = new HikariDataSource();
		luckPermsDataSource.setDriverClassName("com.mysql.jdbc.Driver");
		luckPermsDataSource.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + luckPermsDatabase);
		luckPermsDataSource.setUsername(username);
		luckPermsDataSource.setPassword(password);
		luckPermsDataSource.setPoolName("luckperms-read-pool");
		luckPermsDataSource.setMaximumPoolSize(3);
		luckPermsDataSource.setConnectionTimeout(3000);
		luckPermsDataSource.setLeakDetectionThreshold(3000);
		luckPermsDataSource.setReadOnly(true);

		try {
			createTable();
		} catch (SQLException e) {
			System.out.println("Could not connect to the database. Check your connection settings.");
		}
	}

	@Override
	public boolean isSetup() {
		if (dataSource.isClosed() || dataSource == null) {
			return false;
		}

		return true;
	}

	@Override
	public void insertToken(String token, String discordId) {
		try (Connection c = getConnection();
				PreparedStatement insert = c.prepareStatement(insert_token)) {
			insert.setString(1, token);
			insert.setString(2, discordId);
			insert.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setWaiting(String token, boolean waiting) {
		try (Connection c = getConnection();
				PreparedStatement update = c.prepareStatement(update_token_set_waiting)) {
			update.setBoolean(1, waiting);
			update.setString(2, token);
			update.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setUsed(String token, boolean used) {
		try (Connection c = getConnection();
				PreparedStatement update = c.prepareStatement(update_token_set_used)) {
			update.setBoolean(1, used);
			update.setString(2, token);
			update.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setUUID(String token, UUID uuid) {
		try (Connection c = getConnection();
				PreparedStatement update = c.prepareStatement(update_token_set_uuid)) {
			update.setString(1, uuid.toString());
			update.setString(2, token);
			update.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public List<TokenInfo> getWaiting() {
		List<TokenInfo> tokenInfos = new ArrayList<>();

		try (Connection c = getConnection();
				PreparedStatement query = c.prepareStatement(query_waiting);
				ResultSet rs = query.executeQuery()) {
			while (rs.next()) {
				tokenInfos.add(new TokenInfo(rs.getString("token"), 
						rs.getString("discord_id"), UUID.fromString(rs.getString("uuid"))));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return tokenInfos;
	}

	@Override
	public Map<String, UUID> getIDsWithUUIDs() {
		Map<String, UUID> idsAndUUIDs = new HashMap<>();

		try (Connection c = getConnection();
				PreparedStatement query = c.prepareStatement(query_ids_and_uuids);
				ResultSet rs = query.executeQuery()) {
			
			while (rs.next()) {
				String uuidString = rs.getString("uuid");
				idsAndUUIDs.put(rs.getString("discord_id"), UUID.fromString(uuidString));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return idsAndUUIDs;
	}

	@Override
	public void deleteUser(String discordId) {
		try (Connection c = getConnection();
				PreparedStatement query = c.prepareStatement(delete_user)) {
			query.setString(1, discordId);
			query.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean isUsed(String token) {
		try (Connection c = getConnection();
				PreparedStatement query = c.prepareStatement(query_token)) {
			query.setString(1, token);

			try (ResultSet rs = query.executeQuery()) {
				while (rs.next()) {
					return rs.getBoolean("used");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return true;
	}

	@Override
	public String getUsername(UUID uuid) {
		try (Connection c = getLuckPermsConnection();
				PreparedStatement query = c.prepareStatement(luckperms_query_username)) {
			query.setString(1, uuid.toString());

			try (ResultSet rs = query.executeQuery()) {
				while (rs.next()) {
					return rs.getString("username");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public boolean hasPermission(UUID uuid, String permission) {
		try (Connection c = getLuckPermsConnection();
				PreparedStatement query = c.prepareStatement(luckperms_query_permission)) {
			query.setString(1, uuid.toString());
			query.setString(2, permission);

			try (ResultSet rs = query.executeQuery()) {
				while (rs.next()) {
					return rs.getBoolean("value");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public boolean groupHasPermission(String group, String permission) {
		try (Connection c = getLuckPermsConnection();
				PreparedStatement query = c.prepareStatement(luckperms_query_group_permission)) {
			query.setString(1, group);
			query.setString(2, permission);

			try (ResultSet rs = query.executeQuery()) {
				while (rs.next()) {
					return rs.getBoolean("value");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public Collection<String> getPermissionsFromGroup(String group) {
		List<String> perms = new ArrayList<>();

		try (Connection c = getLuckPermsConnection();
				PreparedStatement query = c.prepareStatement(luckperms_query_group_permission_list)) {
			query.setString(1, group);

			try (ResultSet rs = query.executeQuery()) {
				while (rs.next()) {
					perms.add(rs.getString("permission"));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return perms;
	}

	@Override
	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	@Override
	public Connection getLuckPermsConnection() throws SQLException {
		return luckPermsDataSource.getConnection();
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

	@Override
	public void close() {
		dataSource.close();
		dataSource = null;

		luckPermsDataSource.close();
		luckPermsDataSource = null;
	}

}
