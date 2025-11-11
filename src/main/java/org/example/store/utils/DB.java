package org.example.store.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB {
    public static Connection connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC"); // مهم جدًا
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC Driver not found", e);
        }

        return DriverManager.getConnection("jdbc:sqlite:store.db");
    }

}
