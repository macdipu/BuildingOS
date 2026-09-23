package com.buildingos.subscription.subscription.infrastructure.persistence;

import com.buildingos.subscription.catalog.domain.model.BillingCycle;
import com.buildingos.subscription.subscription.domain.model.GrantSource;
import com.buildingos.subscription.subscription.domain.model.SubjectType;
import com.buildingos.subscription.subscription.domain.model.Subscription;
import com.buildingos.subscription.subscription.domain.model.SubscriptionStatus;
import com.buildingos.subscription.subscription.domain.model.SubscriptionSubject;
import com.buildingos.subscription.subscription.domain.repository.SubscriptionRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSubscriptionRepositoryAdapter implements SubscriptionRepository {
    private final JdbcTemplate jdbc;

    public JdbcSubscriptionRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Optional<Subscription> findActive(SubscriptionSubject subject) {
        return jdbc.query("SELECT id, subject_type, subject_id, plan_id, status, billing_cycle, granted_by, started_at "
                        + "FROM subscription WHERE subject_type = ? AND subject_id = ? AND status = 'ACTIVE'",
                this::map, subject.type().name(), subject.id()).stream().findFirst();
    }

    @Override
    public boolean insertIfNoneActive(Subscription subscription) {
        // The partial unique index is the cross-instance guard; a losing concurrent insert does nothing.
        return jdbc.update("INSERT INTO subscription (id, subject_type, subject_id, plan_id, status, billing_cycle, "
                        + "granted_by, started_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                        + "ON CONFLICT (subject_type, subject_id) WHERE status = 'ACTIVE' DO NOTHING",
                subscription.id(), subscription.subject().type().name(), subscription.subject().id(),
                subscription.planId(), subscription.status().name(), subscription.billingCycle().name(),
                subscription.grantedBy().name(), Timestamp.from(subscription.startedAt())) == 1;
    }

    private Subscription map(ResultSet rs, int row) throws SQLException {
        return new Subscription(rs.getObject("id", UUID.class),
                new SubscriptionSubject(SubjectType.valueOf(rs.getString("subject_type")),
                        rs.getObject("subject_id", UUID.class)),
                rs.getObject("plan_id", UUID.class), SubscriptionStatus.valueOf(rs.getString("status")),
                BillingCycle.valueOf(rs.getString("billing_cycle")), GrantSource.valueOf(rs.getString("granted_by")),
                rs.getTimestamp("started_at").toInstant());
    }
}
