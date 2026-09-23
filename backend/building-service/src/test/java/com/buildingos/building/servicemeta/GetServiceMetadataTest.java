package com.buildingos.building.servicemeta;

import com.buildingos.building.servicemeta.application.getservicemetadata.GetServiceMetadataQuery;
import com.buildingos.building.servicemeta.application.getservicemetadata.GetServiceMetadataService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GetServiceMetadataTest {
    @Test void exposesOnlyServiceIdentity() {
        assertEquals("building-service", new GetServiceMetadataService().execute(new GetServiceMetadataQuery()).service());
    }
}
