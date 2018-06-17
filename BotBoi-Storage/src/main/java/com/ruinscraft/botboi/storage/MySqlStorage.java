package com.ruinscraft.botboi.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.zaxxer.hikari.HikariDataSource;

public class MySqlStorage implements SqlStorage {

	private HikariDataSource dataSource;

	private String create_table;
	private String insert_token;
	private String update_token_set_waiting;
	private String update_token_set_used;
	private String update_token_set_username;
	private String query_waiting;
	private String query_token;

	public MySqlStorage(String host, int port, String database, String username, String password, String botboiTable) {
		create_table = "CREATE TABLE IF NOT EXISTS " + botboiTable + " (token VARCHAR(36), discord_id VARCHAR(36), mc_user VARCHAR(36), waiting BOOL DEFAULT 0, used BOOL DEFAULT 0, UNIQUE (token));";
		insert_token = "INSERT INTO " + botboiTable + " (token, discord_id) VALUES (?, ?);";
		update_token_set_waiting = "UPDATE " + botboiTable + " SET waiting = ? WHERE token = ?;";
		update_token_set_used = "UPDATE " + botboiTable + " SET used = ? WHERE token = ?;";
		update_token_set_username = "UPDATE " + botboiTable + " SET mc_user = ? WHERE token = ?;";
		query_waiting = "SELECT token, discord_id FROM " + botboiTable + " WHERE waiting = 1;";
		query_token = "SELECT * FROM " + botboiTable + " WHERE token = ?;";

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
	public void setUsername(String token, String username) {
		try (Connection c = getConnection();
				PreparedStatement update = c.prepareStatement(update_token_set_username)) {
			update.setString(1, username);
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
				PreparedStatement query = c.prepareStatement(query_waiting)) {
			ResultSet rs = query.executeQuery();

			while (rs.next()) {
				tokenInfos.add(new TokenInfo(rs.getString("token"), 
						rs.getString("discord_id"), rs.getString("mc_user")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return tokenInfos;
	}

	@Override
	public boolean isUsed(String token) {
		try (Connection c = getConnection();
				PreparedStatement query = c.prepareStatement(query_token)) {
			query.setString(1, token);

			ResultSet rs = query.executeQuery();

			while (rs.next()) {
				return rs.getBoolean("used");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return true;
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

	@Override
	public void close() {
		dataSource.close();
		dataSource = null;
	}

	public class TokenInfo {

		private String token;
		private String discord_id;
		private String mc_user;

		public TokenInfo(String token, String discord_id, String mc_user) {
			this.token = token;
			this.discord_id = discord_id;
			this.mc_user = mc_user;
		}

		public String getToken() {
			return token;
		}

		public String getDiscordId() {
			return discord_id;
		}

		public String getMcUser() {
			return mc_user;
		}
	}

}
