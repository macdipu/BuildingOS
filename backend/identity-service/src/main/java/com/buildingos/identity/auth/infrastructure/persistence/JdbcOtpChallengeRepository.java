package com.buildingos.identity.auth.infrastructure.persistence;

import com.buildingos.identity.auth.application.port.out.OtpChallengeRepository;
import com.buildingos.identity.auth.domain.OtpChallenge;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOtpChallengeRepository implements OtpChallengeRepository {
    private final JdbcTemplate jdbc;

    public JdbcOtpChallengeRepository(JdbcTemplate jdbc) {
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
                (rs, rowNum) -> map(rs), attemptId).stream().findFirst();
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

    private static OtpChallenge map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new OtpChallenge(
                rs.getObject("id", UUID.class),
                rs.getString("phone"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("expires_at").toInstant(),
                rs.getInt("attempt_count"),
                rs.getTimestamp("consumed_at") == null ? null : rs.getTimestamp("consumed_at").toInstant());
    }
}
