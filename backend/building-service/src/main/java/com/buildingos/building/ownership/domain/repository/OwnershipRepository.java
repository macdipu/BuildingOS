package com.buildingos.building.ownership.domain.repository;

import com.buildingos.building.ownership.domain.model.OwnershipChange;
import com.buildingos.building.ownership.domain.model.OwnedProperty;
import com.buildingos.building.ownership.domain.model.OwnershipHistory;
import com.buildingos.building.ownership.domain.model.UnitOwnership;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OwnershipRepository {
    /** Locks the unit row (FOR UPDATE) and loads its revision and open allocations. */
    Optional<UnitOwnership> lockUnit(UUID buildingId, UUID unitId);
    /** Applies a change computed from {@link #lockUnit}; callers hold that lock. */
    void apply(OwnershipChange change);
    /** {@code ownerUserId} null = every record; otherwise only that owner's periods and transfers. */
    Optional<OwnershipHistory> history(UUID buildingId, UUID unitId, UUID ownerUserId);
    /** Current allocations without locking, for reads. */
    Optional<UnitOwnership> current(UUID buildingId, UUID unitId);
    /** Current allocations of {@code userId} in buildings where they hold an active membership. */
    List<OwnedProperty> propertiesOf(UUID userId, int page, int size);
    long countPropertiesOf(UUID userId);
    long countOwnedUnits(UUID buildingId, UUID userId);
}
