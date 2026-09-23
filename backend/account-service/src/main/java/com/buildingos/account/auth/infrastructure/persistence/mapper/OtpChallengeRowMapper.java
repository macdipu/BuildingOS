package com.buildingos.account.auth.infrastructure.persistence.mapper;

import com.buildingos.account.auth.domain.model.OtpChallenge;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

/** Maps an {@code otp_challenge} row to the domain {@link OtpChallenge}. */
public final class OtpChallengeRowMapper implements RowMapper<OtpChallenge> {
    @Override
    public OtpChallenge mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new OtpChallenge(
                rs.getObject("id", UUID.class),
                rs.getString("phone"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("expires_at").toInstant(),
                rs.getInt("attempt_count"),
                rs.getTimestamp("consumed_at") == null ? null : rs.getTimestamp("consumed_at").toInstant(),
                rs.getString("code_hash"));
    }
}
