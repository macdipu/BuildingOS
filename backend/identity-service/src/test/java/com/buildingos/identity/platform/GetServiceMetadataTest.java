package com.buildingos.identity.platform;

import com.buildingos.identity.platform.application.usecase.GetServiceMetadataService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GetServiceMetadataTest {
    @Test void exposesOnlyServiceIdentity() {
        assertEquals("identity-service", new GetServiceMetadataService().execute().service());
    }
}
