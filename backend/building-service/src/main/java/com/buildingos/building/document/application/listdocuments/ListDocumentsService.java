package com.buildingos.building.document.application.listdocuments;

import com.buildingos.building.buildingapplication.application.ApplicationAccess;
import com.buildingos.building.buildingapplication.application.ApplicationChanges;
import com.buildingos.building.document.domain.model.ApplicationDocument;
import com.buildingos.building.document.domain.repository.ApplicationDocumentRepository;
import com.buildingos.building.shared.application.Actor;
import java.util.List;

public final class ListDocumentsService implements ListDocumentsUseCase {
    private final ApplicationChanges applications;
    private final ApplicationDocumentRepository documents;

    public ListDocumentsService(ApplicationChanges applications, ApplicationDocumentRepository documents) {
        this.applications = applications;
        this.documents = documents;
    }

    @Override
    public List<ApplicationDocument> execute(Actor actor, ListDocumentsQuery query) {
        var application = applications.load(actor, query.applicationId(), ApplicationAccess.APPLICANT_OR_PLATFORM_ADMIN);
        return documents.findByApplication(application.id());
    }
}
