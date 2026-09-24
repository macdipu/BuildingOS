package com.buildingos.building.ownership.application.removetransferdocument;

import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.ownership.application.TransferDocuments;
import com.buildingos.building.ownership.domain.model.TransferDocument;
import com.buildingos.building.ownership.domain.repository.TransferDocumentRepository;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.shared.domain.model.AuditEntry;
import com.buildingos.building.shared.domain.repository.AuditRepository;
import java.time.Clock;
import java.util.Map;

/**
 * Audited removal (TECH-SPEC-F4): the metadata row is kept and marked removed with actor/time/reason, the transfer
 * is untouched, and the bytes are deleted after commit. A storage failure then leaves an orphan object, never a
 * visible document without bytes.
 */
public final class RemoveTransferDocumentService implements RemoveTransferDocumentUseCase {
    private final TransferDocuments transfers;
    private final TransferDocumentRepository documents;
    private final AuditRepository audit;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public RemoveTransferDocumentService(TransferDocuments transfers, TransferDocumentRepository documents,
            AuditRepository audit, UnitOfWork unitOfWork, Clock clock) {
        this.transfers = transfers;
        this.documents = documents;
        this.audit = audit;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public void execute(Actor actor, RemoveTransferDocumentCommand c) {
        String reason = BuildingApplication.reason(c.reason(), "reason");
        TransferDocument removed = unitOfWork.inTransaction(() -> {
            transfers.requireWritable(actor, c.buildingId(), c.unitId(), c.transferId());
            var document = documents.findActive(c.buildingId(), c.transferId(), c.documentId())
                    .orElseThrow(TransferDocuments::documentNotFound);
            var now = clock.instant();
            documents.markRemoved(document.id(), actor.userId(), now, reason);
            audit.append(AuditEntry.of(c.buildingId(), actor.userId(), "TRANSFER_DOCUMENT_REMOVED",
                    "OWNERSHIP_DOCUMENT", document.id(), reason, Map.of("transferId", c.transferId().toString()),
                    Map.of(), now));
            return document;
        });
        transfers.deleteQuietly(removed.objectKey());
    }
}
