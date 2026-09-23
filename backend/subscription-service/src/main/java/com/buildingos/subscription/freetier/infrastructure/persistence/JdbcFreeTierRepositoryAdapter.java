package com.buildingos.subscription.freetier.infrastructure.persistence;

import com.buildingos.subscription.catalog.domain.model.Entitlements;
import com.buildingos.subscription.freetier.domain.repository.FreeTierRepository;
import com.buildingos.subscription.shared.infrastructure.persistence.JsonColumns;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcFreeTierRepositoryAdapter implements FreeTierRepository {
    private final JdbcTemplate jdbc;
    private final JsonColumns json;

    public JdbcFreeTierRepositoryAdapter(JdbcTemplate jdbc, JsonColumns json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public Entitlements get() {
        String value = jdbc.queryForObject("SELECT entitlements::text FROM free_tier WHERE id = 1", String.class);
        return Entitlements.fromKeys(json.readMap(value));
    }

    @Override
    public void save(Entitlements entitlements) {
        jdbc.update("UPDATE free_tier SET entitlements = ?::jsonb, updated_at = now() WHERE id = 1",
                json.write(entitlements.toKeys()));
    }
}
