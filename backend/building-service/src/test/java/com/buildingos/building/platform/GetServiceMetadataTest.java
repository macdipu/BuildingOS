package com.buildingos.building.platform;

import com.buildingos.building.platform.application.usecase.GetServiceMetadataService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GetServiceMetadataTest {
    @Test void exposesOnlyServiceIdentity() {
        assertEquals("building-service", new GetServiceMetadataService().execute().service());
    }
}
