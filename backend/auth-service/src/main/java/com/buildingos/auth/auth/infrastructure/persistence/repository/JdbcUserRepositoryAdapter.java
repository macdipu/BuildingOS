package com.buildingos.auth.auth.infrastructure.persistence.repository;

import com.buildingos.auth.auth.domain.model.PlatformRole;
import com.buildingos.auth.auth.domain.model.User;
import com.buildingos.auth.auth.domain.repository.UserRepository;
import com.buildingos.auth.auth.infrastructure.persistence.mapper.UserRowMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUserRepositoryAdapter implements UserRepository {
    private final JdbcTemplate jdbc;

    public JdbcUserRepositoryAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        List<UUID> ids = jdbc.query("SELECT id FROM app_user WHERE phone = ?", UserRowMapper.ID, phone);
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

    @Override
    public void grantPlatformRole(UUID userId, PlatformRole role) {
        jdbc.update("""
                INSERT INTO platform_user_role (user_id, role) VALUES (?, ?)
                ON CONFLICT (user_id, role) DO NOTHING
                """, userId, role.name());
    }

    private User load(UUID id, String phone) {
        var createdAt = jdbc.queryForObject("SELECT created_at FROM app_user WHERE id = ?",
                UserRowMapper.CREATED_AT, id);
        Set<PlatformRole> roles = jdbc.query("SELECT role FROM platform_user_role WHERE user_id = ?",
                UserRowMapper.PLATFORM_ROLE, id)
                .stream().collect(Collectors.toUnmodifiableSet());
        return new User(id, phone, createdAt, roles);
    }
}
