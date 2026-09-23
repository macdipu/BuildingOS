package com.buildingos.account.servicemeta.application.getservicemetadata;

import com.buildingos.account.servicemeta.domain.model.ServiceMetadata;

public interface GetServiceMetadataUseCase {
    ServiceMetadata execute(GetServiceMetadataQuery query);
}
