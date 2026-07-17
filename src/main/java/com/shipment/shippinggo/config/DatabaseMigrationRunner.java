package com.shipment.shippinggo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationRunner.class);
    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            log.info("Executing schema updates for TripStatus...");
            // Alter trips table to support new ENUM values or convert to VARCHAR
            jdbcTemplate.execute("ALTER TABLE trips MODIFY COLUMN status ENUM('PREPARING', 'IN_TRANSIT', 'ARRIVED', 'COMPLETED', 'RETURNING', 'RETURNED', 'CANCELLED') NOT NULL DEFAULT 'PREPARING'");
            log.info("Successfully updated trips table status column.");
        } catch (Exception e) {
            log.error("Failed to alter trips table. It might already be updated or is a VARCHAR: {}", e.getMessage());
        }
    }
}
