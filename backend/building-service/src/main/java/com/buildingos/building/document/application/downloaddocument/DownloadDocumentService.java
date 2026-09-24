package com.buildingos.building.document.application.downloaddocument;

import com.buildingos.building.buildingapplication.application.ApplicationAccess;
import com.buildingos.building.buildingapplication.application.ApplicationChanges;
import com.buildingos.building.document.application.DocumentErrors;
import com.buildingos.building.document.application.port.out.DocumentStorage;
import com.buildingos.building.document.domain.repository.ApplicationDocumentRepository;
import com.buildingos.building.shared.application.Actor;

public final class DownloadDocumentService implements DownloadDocumentUseCase {
    private final ApplicationChanges applications;
    private final ApplicationDocumentRepository documents;
    private final DocumentStorage storage;

    public DownloadDocumentService(ApplicationChanges applications, ApplicationDocumentRepository documents,
            DocumentStorage storage) {
        this.applications = applications;
        this.documents = documents;
        this.storage = storage;
    }

    @Override
    public DocumentContent execute(Actor actor, DownloadDocumentQuery query) {
        var application = applications.load(actor, query.applicationId(), ApplicationAccess.APPLICANT_OR_PLATFORM_ADMIN);
        var document = documents.find(application.id(), query.documentId())
                .orElseThrow(() -> DocumentErrors.notFound(query.documentId()));
        return new DocumentContent(document, storage.open(document.objectKey()));
    }
}
