package com.buildingos.building.ownership.application.downloadtransferdocument;

import com.buildingos.building.document.application.port.out.DocumentStorage;
import com.buildingos.building.ownership.application.TransferDocuments;
import com.buildingos.building.ownership.domain.repository.TransferDocumentRepository;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.port.out.UnitOfWork;

public final class DownloadTransferDocumentService implements DownloadTransferDocumentUseCase {
    private final TransferDocuments transfers;
    private final TransferDocumentRepository documents;
    private final DocumentStorage storage;
    private final UnitOfWork unitOfWork;

    public DownloadTransferDocumentService(TransferDocuments transfers, TransferDocumentRepository documents,
            DocumentStorage storage, UnitOfWork unitOfWork) {
        this.transfers = transfers;
        this.documents = documents;
        this.storage = storage;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public TransferDocumentContent execute(Actor actor, DownloadTransferDocumentQuery q) {
        var document = unitOfWork.inTransaction(() -> {
            transfers.requireReadable(actor, q.buildingId(), q.unitId(), q.transferId());
            return documents.findActive(q.buildingId(), q.transferId(), q.documentId())
                    .orElseThrow(TransferDocuments::documentNotFound);
        });
        return new TransferDocumentContent(document, storage.open(document.objectKey()));
    }
}
