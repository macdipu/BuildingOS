package com.buildingos.building.ownership.application.downloadtransferdocument;

import com.buildingos.building.ownership.domain.model.TransferDocument;
import java.io.InputStream;

/** Caller closes {@code content}. */
public record TransferDocumentContent(TransferDocument document, InputStream content) {}
