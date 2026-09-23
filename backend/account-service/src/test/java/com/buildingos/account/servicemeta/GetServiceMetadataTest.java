package com.buildingos.account.servicemeta;

import com.buildingos.account.servicemeta.application.getservicemetadata.GetServiceMetadataQuery;
import com.buildingos.account.servicemeta.application.getservicemetadata.GetServiceMetadataService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GetServiceMetadataTest {
    @Test void exposesOnlyServiceIdentity() {
        assertEquals("account-service", new GetServiceMetadataService().execute(new GetServiceMetadataQuery()).service());
    }
}
