package com.buildingos.backoffice.servicemeta.application.getservicemetadata;

import com.buildingos.backoffice.servicemeta.domain.model.ServiceMetadata;

public final class GetServiceMetadataService implements GetServiceMetadataUseCase {
    @Override
    public ServiceMetadata execute(GetServiceMetadataQuery query) { return new ServiceMetadata("back-office-service"); }
}
