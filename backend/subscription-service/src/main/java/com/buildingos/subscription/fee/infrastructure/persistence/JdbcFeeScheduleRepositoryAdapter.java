package com.buildingos.subscription.fee.infrastructure.persistence;

import com.buildingos.subscription.fee.domain.model.FeeCode;
import com.buildingos.subscription.fee.domain.model.FeeSchedule;
import com.buildingos.subscription.fee.domain.model.Money;
import com.buildingos.subscription.fee.domain.repository.FeeScheduleRepository;
import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcFeeScheduleRepositoryAdapter implements FeeScheduleRepository {
    private final JdbcTemplate jdbc;

    public JdbcFeeScheduleRepositoryAdapter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public Optional<FeeSchedule> find(FeeCode code) {
        return jdbc.query("SELECT code, amount, currency, required, updated_at FROM fee_schedule WHERE code = ?",
                (rs, row) -> new FeeSchedule(FeeCode.valueOf(rs.getString("code")),
                        new Money(rs.getBigDecimal("amount"), rs.getString("currency")), rs.getBoolean("required"),
                        rs.getTimestamp("updated_at").toInstant()), code.name()).stream().findFirst();
    }

    @Override
    public void save(FeeSchedule schedule) {
        jdbc.update("INSERT INTO fee_schedule (code, amount, currency, required, updated_at) VALUES (?, ?, ?, ?, ?) "
                        + "ON CONFLICT (code) DO UPDATE SET amount = EXCLUDED.amount, currency = EXCLUDED.currency, "
                        + "required = EXCLUDED.required, updated_at = EXCLUDED.updated_at",
                schedule.code().name(), schedule.price().amount(), schedule.price().currency(), schedule.required(),
                Timestamp.from(schedule.updatedAt()));
    }
}
