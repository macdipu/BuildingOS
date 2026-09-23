package com.buildingos.account.auth.infrastructure.persistence.repository;

import com.buildingos.account.auth.domain.model.OtpChallenge;
import com.buildingos.account.auth.domain.repository.OtpChallengeRepository;
import com.buildingos.account.auth.infrastructure.persistence.mapper.OtpChallengeRowMapper;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class JdbcOtpChallengeRepositoryAdapter implements OtpChallengeRepository {
    private static final OtpChallengeRowMapper ROW_MAPPER = new OtpChallengeRowMapper();
    private static final String COLUMNS = "id, phone, created_at, expires_at, attempt_count, consumed_at, code_hash";
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public JdbcOtpChallengeRepositoryAdapter(JdbcTemplate jdbc, PlatformTransactionManager transactions) {
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(transactions);
    }

    @Override
    public Optional<OtpChallenge> start(String phone, Instant now, Instant expiresAt,
            Duration cooldown, int maxPerHour) {
        return transaction.execute(status -> {
            // A transaction-scoped database lock serializes starts across service instances.
            // Ignore an optional '+' for locking/counting so it cannot bypass limits.
            String rateKey = phone.startsWith("+") ? phone.substring(1) : phone;
            jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", rs -> { }, rateKey);
            Integer count = jdbc.queryForObject(
                    "SELECT count(*) FROM otp_challenge WHERE phone IN (?, ?) AND created_at > ?",
                    Integer.class, rateKey, "+" + rateKey, Timestamp.from(now.minus(Duration.ofHours(1))));
            Integer recent = jdbc.queryForObject(
                    "SELECT count(*) FROM otp_challenge WHERE phone IN (?, ?) AND created_at > ?",
                    Integer.class, rateKey, "+" + rateKey, Timestamp.from(now.minus(cooldown)));
            if (count >= maxPerHour || recent > 0) return Optional.empty();
            return Optional.of(jdbc.queryForObject(
                    "INSERT INTO otp_challenge (phone, created_at, expires_at) VALUES (?, ?, ?) RETURNING " + COLUMNS,
                    ROW_MAPPER, phone, Timestamp.from(now), Timestamp.from(expiresAt)));
        });
    }

    @Override
    public void saveCodeHash(UUID attemptId, String codeHash) {
        int updated = jdbc.update("UPDATE otp_challenge SET code_hash = ? WHERE id = ? AND code_hash IS NULL AND consumed_at IS NULL",
                codeHash, attemptId);
        if (updated != 1) throw new IllegalStateException("OTP challenge is not issuable");
    }

    @Override
    public Optional<OtpChallenge> find(UUID attemptId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM otp_challenge WHERE id = ?", ROW_MAPPER, attemptId)
                .stream().findFirst();
    }

    @Override
    public OtpChallenge recordAttempt(UUID attemptId) {
        return jdbc.queryForObject(
                "UPDATE otp_challenge SET attempt_count = attempt_count + 1 WHERE id = ? RETURNING " + COLUMNS,
                ROW_MAPPER, attemptId);
    }

    @Override
    public boolean consume(UUID attemptId, Instant now) {
        return jdbc.update("UPDATE otp_challenge SET consumed_at = ? WHERE id = ? AND consumed_at IS NULL AND expires_at > ?",
                Timestamp.from(now), attemptId, Timestamp.from(now)) == 1;
    }
}
