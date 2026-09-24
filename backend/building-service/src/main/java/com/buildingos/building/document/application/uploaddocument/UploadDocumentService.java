package com.buildingos.building.document.application.uploaddocument;

import com.buildingos.building.buildingapplication.application.ApplicationErrors;
import com.buildingos.building.buildingapplication.domain.model.ApplicationNotEditableException;
import com.buildingos.building.buildingapplication.domain.repository.BuildingApplicationRepository;
import com.buildingos.building.document.application.port.out.DocumentStorage;
import com.buildingos.building.document.domain.model.ApplicationDocument;
import com.buildingos.building.document.domain.model.DocumentPolicy;
import com.buildingos.building.document.domain.repository.ApplicationDocumentRepository;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import java.time.Clock;

/**
 * Applicant attaches a file while the application is editable. Runs under the application row lock so the
 * per-application limit holds under concurrency; the object is written first and removed again if the row is not.
 */
public final class UploadDocumentService implements UploadDocumentUseCase {
    private final BuildingApplicationRepository applications;
    private final ApplicationDocumentRepository documents;
    private final DocumentStorage storage;
    private final DocumentPolicy policy;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public UploadDocumentService(BuildingApplicationRepository applications, ApplicationDocumentRepository documents,
            DocumentStorage storage, DocumentPolicy policy, UnitOfWork unitOfWork, Clock clock) {
        this.applications = applications;
        this.documents = documents;
        this.storage = storage;
        this.policy = policy;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public ApplicationDocument execute(Actor actor, UploadDocumentCommand command) {
        var type = policy.accept(command.content());
        return unitOfWork.inTransaction(() -> {
            var application = applications.findByIdForUpdate(command.applicationId())
                    .filter(found -> found.isOwnedBy(actor.userId()))
                    .orElseThrow(() -> ApplicationErrors.notFound(command.applicationId()));
            if (!application.isEditable()) {
                throw new ApplicationNotEditableException(application.status());
            }
            policy.requireRoomFor(documents.countByApplication(application.id()));
            var document = ApplicationDocument.create(application.id(), command.fileName(), type,
                    command.content().length, actor.userId(), clock.instant());
            storage.put(document.objectKey(), command.content(), type.contentType());
            try {
                documents.insert(document);
            } catch (RuntimeException rowFailed) {
                storage.delete(document.objectKey());
                throw rowFailed;
            }
            return document;
        });
    }
}
