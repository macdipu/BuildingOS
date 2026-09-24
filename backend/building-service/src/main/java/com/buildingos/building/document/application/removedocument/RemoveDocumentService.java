package com.buildingos.building.document.application.removedocument;

import com.buildingos.building.buildingapplication.application.ApplicationErrors;
import com.buildingos.building.buildingapplication.domain.model.ApplicationNotEditableException;
import com.buildingos.building.buildingapplication.domain.repository.BuildingApplicationRepository;
import com.buildingos.building.document.application.DocumentErrors;
import com.buildingos.building.document.application.port.out.DocumentStorage;
import com.buildingos.building.document.domain.model.ApplicationDocument;
import com.buildingos.building.document.domain.repository.ApplicationDocumentRepository;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.port.out.UnitOfWork;

/** Row first, object after commit: a storage failure can leave an orphan object, never a row without bytes. */
public final class RemoveDocumentService implements RemoveDocumentUseCase {
    private final BuildingApplicationRepository applications;
    private final ApplicationDocumentRepository documents;
    private final DocumentStorage storage;
    private final UnitOfWork unitOfWork;

    public RemoveDocumentService(BuildingApplicationRepository applications, ApplicationDocumentRepository documents,
            DocumentStorage storage, UnitOfWork unitOfWork) {
        this.applications = applications;
        this.documents = documents;
        this.storage = storage;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public void execute(Actor actor, RemoveDocumentCommand command) {
        ApplicationDocument removed = unitOfWork.inTransaction(() -> {
            var application = applications.findByIdForUpdate(command.applicationId())
                    .filter(found -> found.isOwnedBy(actor.userId()))
                    .orElseThrow(() -> ApplicationErrors.notFound(command.applicationId()));
            if (!application.isEditable()) {
                throw new ApplicationNotEditableException(application.status());
            }
            var document = documents.find(application.id(), command.documentId())
                    .orElseThrow(() -> DocumentErrors.notFound(command.documentId()));
            documents.delete(document.id());
            return document;
        });
        storage.delete(removed.objectKey());
    }
}
