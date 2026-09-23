package com.buildingos.account.auth.infrastructure.persistence.repository;

import com.buildingos.account.auth.domain.model.OtpChallenge;
import com.buildingos.account.auth.domain.repository.OtpChallengeRepository;
import com.buildingos.account.auth.infrastructure.persistence.mapper.OtpChallengeRowMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOtpChallengeRepositoryAdapter implements OtpChallengeRepository {
    private static final OtpChallengeRowMapper ROW_MAPPER = new OtpChallengeRowMapper();

    private final JdbcTemplate jdbc;

    public JdbcOtpChallengeRepositoryAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public OtpChallenge start(String phone, Instant now, Instant expiresAt) {
        UUID id = jdbc.queryForObject(
                "INSERT INTO otp_challenge (phone, created_at, expires_at) VALUES (?, ?, ?) RETURNING id",
                UUID.class, phone, Timestamp.from(now), Timestamp.from(expiresAt));
        return new OtpChallenge(id, phone, now, expiresAt, 0, null);
    }

    @Override
    public Optional<OtpChallenge> find(UUID attemptId) {
        return jdbc.query(
                "SELECT id, phone, created_at, expires_at, attempt_count, consumed_at FROM otp_challenge WHERE id = ?",
                ROW_MAPPER, attemptId).stream().findFirst();
    }

    @Override
    public OtpChallenge recordAttempt(UUID attemptId) {
        jdbc.update("UPDATE otp_challenge SET attempt_count = attempt_count + 1 WHERE id = ?", attemptId);
        return find(attemptId).orElseThrow(() -> new IllegalStateException("Challenge disappeared: " + attemptId));
    }

    @Override
    public boolean consume(UUID attemptId, Instant now) {
        int updated = jdbc.update(
                "UPDATE otp_challenge SET consumed_at = ? WHERE id = ? AND consumed_at IS NULL",
                Timestamp.from(now), attemptId);
        return updated == 1;
    }
}
