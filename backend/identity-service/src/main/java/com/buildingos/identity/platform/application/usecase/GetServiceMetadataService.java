package com.buildingos.identity.platform.application.usecase;

import com.buildingos.identity.platform.application.port.in.GetServiceMetadata;
import com.buildingos.identity.platform.domain.ServiceMetadata;

public final class GetServiceMetadataService implements GetServiceMetadata {
    @Override
    public ServiceMetadata execute() { return new ServiceMetadata("identity-service"); }
}
