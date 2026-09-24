package com.buildingos.building.document.application.downloaddocument;

import com.buildingos.building.document.domain.model.ApplicationDocument;
import java.io.InputStream;

/** Caller closes {@code content}. */
public record DocumentContent(ApplicationDocument document, InputStream content) {}
