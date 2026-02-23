package com.hirely.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database connection manager for Hirely application
 * Manages MySQL database connections using singleton pattern
 */
public class DatabaseConnection {

    // Database configuration
    private static final String URL = "jdbc:mysql://localhost:3306/hirely_db?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // Default for WAMP/XAMPP (empty password)

    // Singleton connection instance
    private static Connection connection = null;

    /**
     * Private constructor to prevent instantiation
     */
    private DatabaseConnection() {
        // Private constructor
    }

    /**
     * Get database connection (creates new connection if needed)
     * @return Connection object
     */
    public static Connection getConnection() {
        try {
            // Check if connection exists and is valid
            if (connection == null || connection.isClosed()) {
                // Load MySQL JDBC Driver
                Class.forName("com.mysql.cj.jdbc.Driver");

                // Establish connection
                connection = DriverManager.getConnection(URL, USER, PASSWORD);

                System.out.println("✅ Database connection established successfully!");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("❌ ERROR: MySQL JDBC Driver not found!");
            System.err.println("   Make sure mysql-connector-java is in your dependencies.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("❌ ERROR: Could not connect to database!");
            System.err.println("   Please check:");
            System.err.println("   1. WAMP/XAMPP is running");
            System.err.println("   2. MySQL service is started");
            System.err.println("   3. Database 'hirely_db' exists");
            System.err.println("   4. Username and password are correct");
            e.printStackTrace();
        }
        return connection;
    }

    /**
     * Close database connection
     */
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✅ Database connection closed successfully!");
            }
        } catch (SQLException e) {
            System.err.println("❌ ERROR: Could not close database connection!");
            e.printStackTrace();
        }
    }

    /**
     * Test if connection is active
     * @return true if connection is active, false otherwise
     */
    public static boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
