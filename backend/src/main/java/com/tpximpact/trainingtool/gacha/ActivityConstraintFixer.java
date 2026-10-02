package com.tpximpact.trainingtool.gacha;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Hibernate adds a CHECK constraint listing the allowed values when it first creates an enum column,
 * and ddl-auto=update never widens it. Databases created before the gacha existed would reject the
 * new GACHA_* activities, so on PostgreSQL we drop that constraint (the enum is still enforced in Java).
 */
@Component
@Order(0)
public class ActivityConstraintFixer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ActivityConstraintFixer.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;

    public ActivityConstraintFixer(DataSource dataSource, JdbcTemplate jdbc) {
        this.dataSource = dataSource;
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection c = dataSource.getConnection()) {
            if (!c.getMetaData().getDatabaseProductName().toLowerCase().contains("postgres")) return;
        } catch (Exception e) {
            return;
        }
        try {
            jdbc.execute("ALTER TABLE IF EXISTS activity_log DROP CONSTRAINT IF EXISTS activity_log_activity_check");
        } catch (Exception e) {
            log.warn("Could not relax activity_log constraint: {}", e.getMessage());
        }
    }
}
