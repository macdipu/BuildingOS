package com.buildingos.account.servicemeta.application.getservicemetadata;

import com.buildingos.account.servicemeta.domain.model.ServiceMetadata;

public final class GetServiceMetadataService implements GetServiceMetadataUseCase {
    @Override
    public ServiceMetadata execute(GetServiceMetadataQuery query) { return new ServiceMetadata("account-service"); }
}
