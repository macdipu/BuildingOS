package com.buildingos.auth.servicemeta;

import com.buildingos.auth.servicemeta.application.getservicemetadata.GetServiceMetadataQuery;
import com.buildingos.auth.servicemeta.application.getservicemetadata.GetServiceMetadataService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GetServiceMetadataTest {
    @Test void exposesOnlyServiceIdentity() {
        assertEquals("auth-service", new GetServiceMetadataService().execute(new GetServiceMetadataQuery()).service());
    }
}
