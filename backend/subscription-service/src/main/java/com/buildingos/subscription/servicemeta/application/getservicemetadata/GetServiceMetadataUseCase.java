package com.buildingos.subscription.servicemeta.application.getservicemetadata;

import com.buildingos.subscription.servicemeta.domain.model.ServiceMetadata;

public interface GetServiceMetadataUseCase {
    ServiceMetadata execute(GetServiceMetadataQuery query);
}
