package com.buildingos.building.ownership.application.listmyproperties;

import com.buildingos.building.ownership.domain.model.OwnedProperty;
import com.buildingos.building.ownership.domain.repository.OwnershipRepository;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.Page;
import com.buildingos.building.shared.application.port.out.UnitOfWork;

/** My Properties: only real current allocations in buildings where the caller is still a member (UO-08). */
public final class ListMyPropertiesService implements ListMyPropertiesUseCase {
    private final OwnershipRepository ownership;
    private final UnitOfWork unitOfWork;

    public ListMyPropertiesService(OwnershipRepository ownership, UnitOfWork unitOfWork) {
        this.ownership = ownership;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public Page<OwnedProperty> execute(Actor actor, ListMyPropertiesQuery query) {
        Page.validate(query.page(), query.size());
        return unitOfWork.inTransaction(() -> new Page<>(ownership.propertiesOf(actor.userId(), query.page(),
                query.size()), query.page(), query.size(), ownership.countPropertiesOf(actor.userId())));
    }
}
