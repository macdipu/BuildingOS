package com.buildingos.subscription.servicemeta;

import com.buildingos.subscription.servicemeta.application.getservicemetadata.GetServiceMetadataQuery;
import com.buildingos.subscription.servicemeta.application.getservicemetadata.GetServiceMetadataService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GetServiceMetadataTest {
    @Test void exposesOnlyServiceIdentity() {
        assertEquals("subscription-service", new GetServiceMetadataService().execute(new GetServiceMetadataQuery()).service());
    }
}
