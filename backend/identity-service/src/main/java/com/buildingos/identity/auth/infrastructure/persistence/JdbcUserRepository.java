package com.buildingos.identity.auth.infrastructure.persistence;

import com.buildingos.identity.auth.application.port.out.UserRepository;
import com.buildingos.identity.auth.domain.PlatformRole;
import com.buildingos.identity.auth.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUserRepository implements UserRepository {
    private final JdbcTemplate jdbc;

    public JdbcUserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        List<UUID> ids = jdbc.query("SELECT id FROM app_user WHERE phone = ?",
                (rs, rowNum) -> rs.getObject("id", UUID.class), phone);
        if (ids.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(load(ids.get(0), phone));
    }

    @Override
    public User findOrCreateByPhone(String phone) {
        return findByPhone(phone).orElseGet(() -> {
            try {
                UUID id = jdbc.queryForObject(
                        "INSERT INTO app_user (phone) VALUES (?) RETURNING id", UUID.class, phone);
                return load(id, phone);
            } catch (DataIntegrityViolationException raceLostToConcurrentInsert) {
                return findByPhone(phone).orElseThrow(() -> raceLostToConcurrentInsert);
            }
        });
    }

    private User load(UUID id, String phone) {
        var createdAt = jdbc.queryForObject("SELECT created_at FROM app_user WHERE id = ?",
                (rs, rowNum) -> rs.getTimestamp("created_at").toInstant(), id);
        Set<PlatformRole> roles = jdbc.query("SELECT role FROM platform_user_role WHERE user_id = ?",
                (rs, rowNum) -> PlatformRole.valueOf(rs.getString("role")), id)
                .stream().collect(Collectors.toUnmodifiableSet());
        return new User(id, phone, createdAt, roles);
    }
}
