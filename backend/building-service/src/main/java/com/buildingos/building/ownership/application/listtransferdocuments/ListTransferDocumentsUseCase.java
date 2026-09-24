package com.buildingos.building.ownership.application.listtransferdocuments;

import com.buildingos.building.ownership.domain.model.TransferDocument;
import com.buildingos.building.shared.application.Actor;
import java.util.List;

public interface ListTransferDocumentsUseCase {
    List<TransferDocument> execute(Actor actor, ListTransferDocumentsQuery query);
}
