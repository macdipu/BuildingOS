package com.buildingos.building.ownership;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.buildingos.building.building.domain.model.BuildingRole;
import com.buildingos.building.document.application.port.out.DocumentStorage;
import com.buildingos.building.document.domain.model.DocumentPolicy;
import com.buildingos.building.membership.application.BuildingGrant;
import com.buildingos.building.ownership.application.TransferDocuments;
import com.buildingos.building.ownership.application.uploadtransferdocument.UploadTransferDocumentCommand;
import com.buildingos.building.ownership.application.uploadtransferdocument.UploadTransferDocumentService;
import com.buildingos.building.ownership.domain.model.OwnershipTransfer;
import com.buildingos.building.ownership.domain.model.Share;
import com.buildingos.building.ownership.domain.repository.TransferDocumentRepository;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.DependencyUnavailableException;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.shared.domain.repository.AuditRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** F4-T5b: a failed metadata write removes the staged object; a failed object write never reaches metadata. */
class UploadTransferDocumentServiceTest {
    private static final byte[] PDF = "%PDF-1.7\n1 0 obj".getBytes(StandardCharsets.US_ASCII);
    private static final Instant NOW = Instant.parse("2026-09-24T10:00:00Z");
    private static final UnitOfWork DIRECT = new UnitOfWork() {
        @Override
        public <T> T inTransaction(Supplier<T> work) { return work.get(); }
    };

    private final TransferDocuments transfers = mock(TransferDocuments.class);
    private final TransferDocumentRepository documents = mock(TransferDocumentRepository.class);
    private final DocumentStorage storage = mock(DocumentStorage.class);
    private final AuditRepository audit = mock(AuditRepository.class);
    private final UploadTransferDocumentService service = new UploadTransferDocumentService(transfers, documents,
            storage, new DocumentPolicy(4096, 10), audit, DIRECT, Clock.fixed(NOW, ZoneOffset.UTC), 10);
    private final Actor admin = new Actor(UUID.randomUUID(), Set.of());
    private UploadTransferDocumentCommand command;

    @BeforeEach
    void target() {
        UUID building = UUID.randomUUID();
        UUID unit = UUID.randomUUID();
        var transfer = new OwnershipTransfer(UUID.randomUUID(), building, unit, UUID.randomUUID(), UUID.randomUUID(),
                Share.of(new BigDecimal("30")), LocalDate.of(2026, 9, 24), NOW, 2, UUID.randomUUID(), "Sale", null);
        var grant = new BuildingGrant(null, Set.of(BuildingRole.BUILDING_ADMIN), false);
        when(transfers.requireWritable(admin, building, unit, transfer.id()))
                .thenReturn(new TransferDocuments.Target(grant, transfer));
        command = new UploadTransferDocumentCommand(building, unit, transfer.id(), "deed.pdf", PDF,
                null);
    }

    @Test
    void failedMetadataWriteRemovesTheStagedObject() {
        doThrow(new IllegalStateException("insert failed")).when(documents).insert(any());

        assertThatThrownBy(() -> service.execute(admin, command)).hasMessage("insert failed");

        verify(storage).put(anyString(), eq(PDF), eq("application/pdf"));
        verify(transfers).deleteQuietly(anyString());
    }

    @Test
    void failedObjectWriteLeavesNoMetadataOrCleanup() {
        doThrow(new DependencyUnavailableException("Document storage", new RuntimeException("down")))
                .when(storage).put(anyString(), any(), anyString());

        assertThatThrownBy(() -> service.execute(admin, command)).isInstanceOf(DependencyUnavailableException.class);

        verify(documents, never()).insert(any());
        verify(audit, never()).append(any());
        verify(transfers, never()).deleteQuietly(anyString());
    }
}
