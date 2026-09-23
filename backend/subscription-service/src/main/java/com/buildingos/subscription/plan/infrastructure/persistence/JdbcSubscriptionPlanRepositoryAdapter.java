package com.buildingos.subscription.plan.infrastructure.persistence;

import com.buildingos.subscription.catalog.domain.model.BillingCycle;
import com.buildingos.subscription.catalog.domain.model.Entitlements;
import com.buildingos.subscription.plan.domain.model.PlanCode;
import com.buildingos.subscription.plan.domain.model.PlanStatus;
import com.buildingos.subscription.plan.domain.model.SubscriptionPlan;
import com.buildingos.subscription.plan.domain.repository.SubscriptionPlanRepository;
import com.buildingos.subscription.shared.infrastructure.persistence.JsonColumns;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSubscriptionPlanRepositoryAdapter implements SubscriptionPlanRepository {
    private static final String COLUMNS = "id, code, name, status, billing_cycles::text AS billing_cycles, "
            + "self_service, entitlements::text AS entitlements, created_at, updated_at";
    private final JdbcTemplate jdbc;
    private final JsonColumns json;

    public JdbcSubscriptionPlanRepositoryAdapter(JdbcTemplate jdbc, JsonColumns json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public boolean insert(SubscriptionPlan plan) {
        return jdbc.update("INSERT INTO subscription_plan (id, code, name, status, billing_cycles, self_service, "
                        + "entitlements, created_at, updated_at) VALUES (?, ?, ?, ?, ?::jsonb, ?, ?::jsonb, ?, ?) "
                        + "ON CONFLICT (code) DO NOTHING",
                plan.id(), plan.code().value(), plan.name(), plan.status().name(), cycles(plan), plan.selfService(),
                json.write(plan.entitlements().toKeys()), Timestamp.from(plan.createdAt()),
                Timestamp.from(plan.updatedAt())) == 1;
    }

    @Override
    public void update(SubscriptionPlan plan) {
        jdbc.update("UPDATE subscription_plan SET name = ?, status = ?, billing_cycles = ?::jsonb, self_service = ?, "
                        + "entitlements = ?::jsonb, updated_at = ? WHERE id = ?",
                plan.name(), plan.status().name(), cycles(plan), plan.selfService(),
                json.write(plan.entitlements().toKeys()), Timestamp.from(plan.updatedAt()), plan.id());
    }

    @Override
    public Optional<SubscriptionPlan> findById(UUID id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM subscription_plan WHERE id = ?", this::map, id)
                .stream().findFirst();
    }

    @Override
    public Optional<SubscriptionPlan> findByIdForUpdate(UUID id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM subscription_plan WHERE id = ? FOR UPDATE", this::map, id)
                .stream().findFirst();
    }

    @Override
    public List<SubscriptionPlan> findAll() {
        return jdbc.query("SELECT " + COLUMNS + " FROM subscription_plan ORDER BY created_at, code", this::map);
    }

    private String cycles(SubscriptionPlan plan) {
        return json.write(plan.billingCycles().stream().map(Enum::name).sorted().toList());
    }

    private SubscriptionPlan map(ResultSet rs, int row) throws SQLException {
        return new SubscriptionPlan(rs.getObject("id", UUID.class), new PlanCode(rs.getString("code")),
                rs.getString("name"), PlanStatus.valueOf(rs.getString("status")),
                json.readList(rs.getString("billing_cycles")).stream().map(BillingCycle::valueOf)
                        .collect(Collectors.toSet()),
                rs.getBoolean("self_service"), Entitlements.fromKeys(json.readMap(rs.getString("entitlements"))),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }
}
