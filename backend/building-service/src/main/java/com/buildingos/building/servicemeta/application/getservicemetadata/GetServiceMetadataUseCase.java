package com.buildingos.building.servicemeta.application.getservicemetadata;

import com.buildingos.building.servicemeta.domain.model.ServiceMetadata;

public interface GetServiceMetadataUseCase {
    ServiceMetadata execute(GetServiceMetadataQuery query);
}
