package com.buildingos.backoffice.servicemeta.application.getservicemetadata;

import com.buildingos.backoffice.servicemeta.domain.model.ServiceMetadata;

public interface GetServiceMetadataUseCase {
    ServiceMetadata execute(GetServiceMetadataQuery query);
}
