package com.buildingos.identity.auth.infrastructure.seed;

import com.buildingos.identity.auth.domain.PlatformRole;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Deploy-time seed bootstrap for the first SUPER_ADMIN (BOS-002 D-02, carried into
 * BOS-010): seed phone {@value #SEED_PHONE}. Authenticates via the ordinary phone+OTP
 * flow like any user; there is no admin panel or password anywhere in this design.
 * Local/test profile only — this is a development convenience, not a production
 * provisioning mechanism. Idempotent: safe to run on every startup.
 *
 * <p>Must never abort application startup: database outage is a readiness concern
 * (reported by the existing health/readiness probe), not a reason to fail liveness. A
 * failed seed attempt is logged and retried on the next restart.
 */
@Component
@Profile({"local", "test"})
public final class SuperAdminSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SuperAdminSeeder.class);
    static final String SEED_PHONE = "01306999005";

    private final JdbcTemplate jdbc;

    public SuperAdminSeeder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            UUID userId = jdbc.query("SELECT id FROM app_user WHERE phone = ?",
                    (rs, rowNum) -> rs.getObject("id", java.util.UUID.class), SEED_PHONE)
                    .stream().findFirst()
                    .orElseGet(() -> jdbc.queryForObject(
                            "INSERT INTO app_user (phone) VALUES (?) RETURNING id", UUID.class, SEED_PHONE));
            jdbc.update("""
                    INSERT INTO platform_user_role (user_id, role) VALUES (?, ?)
                    ON CONFLICT (user_id, role) DO NOTHING
                    """, userId, PlatformRole.SUPER_ADMIN.name());
        } catch (DataAccessException databaseUnavailable) {
            log.warn("Could not seed first SUPER_ADMIN (database unavailable); will retry on next startup",
                    databaseUnavailable);
        }
    }
}
