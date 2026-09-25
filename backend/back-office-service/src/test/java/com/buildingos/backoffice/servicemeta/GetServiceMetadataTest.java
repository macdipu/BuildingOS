package com.buildingos.backoffice.servicemeta;

import com.buildingos.backoffice.servicemeta.application.getservicemetadata.GetServiceMetadataQuery;
import com.buildingos.backoffice.servicemeta.application.getservicemetadata.GetServiceMetadataService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GetServiceMetadataTest {
    @Test void exposesOnlyServiceIdentity() {
        assertEquals("back-office-service", new GetServiceMetadataService().execute(new GetServiceMetadataQuery()).service());
    }
}
