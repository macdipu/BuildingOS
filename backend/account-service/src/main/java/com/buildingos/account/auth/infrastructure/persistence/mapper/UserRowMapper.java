package com.buildingos.account.auth.infrastructure.persistence.mapper;

import com.buildingos.account.auth.domain.model.PlatformRole;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;

/**
 * Column mappers for the {@code app_user} / {@code platform_user_role} rows a
 * {@link com.buildingos.account.auth.domain.model.User} is assembled from (one user row plus its role rows).
 */
public final class UserRowMapper {
    public static final RowMapper<UUID> ID = (rs, rowNum) -> rs.getObject("id", UUID.class);
    public static final RowMapper<Instant> CREATED_AT = (rs, rowNum) -> rs.getTimestamp("created_at").toInstant();
    public static final RowMapper<PlatformRole> PLATFORM_ROLE =
            (rs, rowNum) -> PlatformRole.valueOf(rs.getString("role"));

    private UserRowMapper() {
    }
}
