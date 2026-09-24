package com.buildingos.building.document.application.downloaddocument;

import com.buildingos.building.shared.application.Actor;

public interface DownloadDocumentUseCase {
    DocumentContent execute(Actor actor, DownloadDocumentQuery query);
}
