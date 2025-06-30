package com.moneysab.cardexis.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Database connection monitor for tracking connection health.
 *
 * This component provides database connectivity monitoring and logging
 * to help diagnose connection issues.
 *
 * @author EL.AHGOUNE
 * @version 1.0.0
 * @since 2024
 */
@Component
public class DatabaseHealthIndicator {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthIndicator.class);
    
    @Autowired
    private DataSource dataSource;
    
    /**
     * Check database connectivity and log the status.
     *
     * @return true if database is accessible, false otherwise
     */
    public boolean checkDatabaseHealth() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1)) {
                logger.debug("Database connection is healthy - URL: {}, Driver: {}",
                    connection.getMetaData().getURL(),
                    connection.getMetaData().getDriverName());
                return true;
            } else {
                logger.warn("Database connection is invalid");
                return false;
            }
        } catch (SQLException e) {
            logger.error("Database connection failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Get detailed database connection information.
     *
     * @return connection details as a string
     */
    public String getDatabaseInfo() {
        try (Connection connection = dataSource.getConnection()) {
            return String.format("Database: %s, Driver: %s, Valid: %s",
                connection.getMetaData().getURL(),
                connection.getMetaData().getDriverName(),
                connection.isValid(1));
        } catch (SQLException e) {
            return "Database connection failed: " + e.getMessage();
        }
    }
}