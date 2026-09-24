package com.buildingos.building.unit.infrastructure.persistence;

import com.buildingos.building.unit.domain.model.Unit;
import com.buildingos.building.unit.domain.model.UnitBatch;
import com.buildingos.building.unit.domain.repository.UnitBatchRepository;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUnitBatchRepositoryAdapter implements UnitBatchRepository {
    private final JdbcTemplate jdbc;
    private final JdbcUnitRepositoryAdapter units;

    public JdbcUnitBatchRepositoryAdapter(JdbcTemplate jdbc, JdbcUnitRepositoryAdapter units) {
        this.jdbc = jdbc;
        this.units = units;
    }

    @Override
    public void insert(UnitBatch batch, List<Unit> created) {
        jdbc.update("INSERT INTO building_unit_batch (id, building_id, created_by, row_count, created_at) "
                        + "VALUES (?, ?, ?, ?, ?)", batch.id(), batch.buildingId(), batch.createdBy(), batch.rowCount(),
                Timestamp.from(batch.createdAt()));
        jdbc.batchUpdate("INSERT INTO building_unit (id, building_id, floor_id, number, normalized_number, unit_type, "
                        + "area_sqft, bedrooms, default_maintenance_rate, notes, version, created_at, updated_at, "
                        + "batch_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                created.stream().map(u -> {
                    var d = u.details();
                    return new Object[] {u.id(), u.buildingId(), d.floorId(), d.number(), d.normalizedNumber(),
                            d.type().name(), d.areaSqft(), d.bedrooms(), d.defaultMaintenanceRate(), d.notes(),
                            u.version(), Timestamp.from(u.createdAt()), Timestamp.from(u.updatedAt()), batch.id()};
                }).toList());
    }

    @Override
    public List<Unit> findUnits(UUID buildingId, UUID batchId) {
        return jdbc.query("SELECT " + JdbcUnitRepositoryAdapter.columns() + " FROM building_unit "
                + "WHERE building_id = ? AND batch_id = ? ORDER BY normalized_number, id", units::mapRow, buildingId,
                batchId);
    }
}
