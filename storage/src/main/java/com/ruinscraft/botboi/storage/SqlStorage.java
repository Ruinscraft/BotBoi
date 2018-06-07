package com.ruinscraft.botboi.storage;

import java.sql.Connection;
import java.sql.SQLException;

public interface SqlStorage extends Storage {

	Connection getConnection() throws SQLException;
	
	void createTable() throws SQLException;
	
}
