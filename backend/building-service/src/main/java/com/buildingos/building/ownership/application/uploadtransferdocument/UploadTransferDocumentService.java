package com.buildingos.building.ownership.application.uploadtransferdocument;

import com.buildingos.building.document.application.port.out.DocumentStorage;
import com.buildingos.building.document.domain.model.DocumentPolicy;
import com.buildingos.building.ownership.application.TransferDocuments;
import com.buildingos.building.ownership.domain.model.TransferDocument;
import com.buildingos.building.ownership.domain.repository.TransferDocumentRepository;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.BusinessRuleException;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.shared.domain.model.AuditEntry;
import com.buildingos.building.shared.domain.repository.AuditRepository;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Admin attaches a document to a transfer (UO-13) with the application-document content/size boundary. The object is
 * stored before the metadata row commits; if anything after that fails, the staged object is removed best-effort so
 * no row ever points at missing bytes.
 */
public final class UploadTransferDocumentService implements UploadTransferDocumentUseCase {
    private final TransferDocuments transfers;
    private final TransferDocumentRepository documents;
    private final DocumentStorage storage;
    private final DocumentPolicy policy;
    private final AuditRepository audit;
    private final UnitOfWork unitOfWork;
    private final Clock clock;
    private final int maxPerTransfer;

    public UploadTransferDocumentService(TransferDocuments transfers, TransferDocumentRepository documents,
            DocumentStorage storage, DocumentPolicy policy, AuditRepository audit, UnitOfWork unitOfWork, Clock clock,
            int maxPerTransfer) {
        this.transfers = transfers;
        this.documents = documents;
        this.storage = storage;
        this.policy = policy;
        this.audit = audit;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
        this.maxPerTransfer = maxPerTransfer;
    }

    @Override
    public TransferDocument execute(Actor actor, UploadTransferDocumentCommand c) {
        var type = policy.accept(c.content());
        AtomicReference<String> staged = new AtomicReference<>();
        try {
            return unitOfWork.inTransaction(() -> {
                var target = transfers.requireWritable(actor, c.buildingId(), c.unitId(), c.transferId());
                String reason = target.grant().auditReason(c.reason(), "Transfer document attached");
                if (documents.countActive(c.buildingId(), c.transferId()) >= maxPerTransfer) {
                    throw BusinessRuleException.conflict("DOCUMENT_LIMIT_REACHED",
                            "A transfer can hold at most " + maxPerTransfer + " documents");
                }
                var now = clock.instant();
                var document = TransferDocument.create(target.transfer(), c.fileName(), type, c.content().length,
                        actor.userId(), now);
                storage.put(document.objectKey(), c.content(), type.contentType());
                staged.set(document.objectKey());
                documents.insert(document);
                audit.append(AuditEntry.of(c.buildingId(), actor.userId(), "TRANSFER_DOCUMENT_ATTACHED",
                        "OWNERSHIP_DOCUMENT", document.id(), reason, Map.of(), Map.of(
                                "transferId", c.transferId().toString(), "contentType", type.contentType(),
                                "sizeBytes", Long.toString(document.sizeBytes())), now));
                return document;
            });
        } catch (RuntimeException failed) {
            if (staged.get() != null) {
                transfers.deleteQuietly(staged.get());
            }
            throw failed;
        }
    }
}
