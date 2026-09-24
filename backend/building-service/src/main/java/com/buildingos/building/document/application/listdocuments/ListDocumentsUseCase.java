package com.buildingos.building.document.application.listdocuments;

import com.buildingos.building.document.domain.model.ApplicationDocument;
import com.buildingos.building.shared.application.Actor;
import java.util.List;

public interface ListDocumentsUseCase {
    List<ApplicationDocument> execute(Actor actor, ListDocumentsQuery query);
}
