package com.buildingos.auth.servicemeta.application.getservicemetadata;

import com.buildingos.auth.servicemeta.domain.model.ServiceMetadata;

public final class GetServiceMetadataService implements GetServiceMetadataUseCase {
    @Override
    public ServiceMetadata execute(GetServiceMetadataQuery query) { return new ServiceMetadata("auth-service"); }
}
