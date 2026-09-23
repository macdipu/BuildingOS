package com.buildingos.subscription.servicemeta.application.getservicemetadata;

import com.buildingos.subscription.servicemeta.domain.model.ServiceMetadata;

public final class GetServiceMetadataService implements GetServiceMetadataUseCase {
    @Override
    public ServiceMetadata execute(GetServiceMetadataQuery query) { return new ServiceMetadata("subscription-service"); }
}
