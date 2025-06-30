package com.moneysab.cardexis.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseChecker implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseChecker(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            String result = jdbcTemplate.queryForObject("SELECT 'Connection Successful!'", String.class);
            System.out.println(result);
        } catch (Exception e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }
}
