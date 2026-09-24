package com.buildingos.building.ownership.application.listtransferdocuments;

import com.buildingos.building.ownership.application.TransferDocuments;
import com.buildingos.building.ownership.domain.model.TransferDocument;
import com.buildingos.building.ownership.domain.repository.TransferDocumentRepository;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import java.util.List;

public final class ListTransferDocumentsService implements ListTransferDocumentsUseCase {
    private final TransferDocuments transfers;
    private final TransferDocumentRepository documents;
    private final UnitOfWork unitOfWork;

    public ListTransferDocumentsService(TransferDocuments transfers, TransferDocumentRepository documents,
            UnitOfWork unitOfWork) {
        this.transfers = transfers;
        this.documents = documents;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public List<TransferDocument> execute(Actor actor, ListTransferDocumentsQuery q) {
        return unitOfWork.inTransaction(() -> {
            transfers.requireReadable(actor, q.buildingId(), q.unitId(), q.transferId());
            return documents.listActive(q.buildingId(), q.transferId());
        });
    }
}
