package com.buildingos.auth.servicemeta.application.getservicemetadata;

import com.buildingos.auth.servicemeta.domain.model.ServiceMetadata;

public interface GetServiceMetadataUseCase {
    ServiceMetadata execute(GetServiceMetadataQuery query);
}
