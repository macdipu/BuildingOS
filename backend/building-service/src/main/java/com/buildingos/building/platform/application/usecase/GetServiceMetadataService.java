package com.buildingos.building.platform.application.usecase;

import com.buildingos.building.platform.application.port.in.GetServiceMetadata;
import com.buildingos.building.platform.domain.ServiceMetadata;

public final class GetServiceMetadataService implements GetServiceMetadata {
    @Override
    public ServiceMetadata execute() { return new ServiceMetadata("building-service"); }
}
