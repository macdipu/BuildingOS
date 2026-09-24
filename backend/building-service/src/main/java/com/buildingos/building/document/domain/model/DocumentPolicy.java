package com.buildingos.building.document.domain.model;

/** Configurable upload limits (AP-05). */
public record DocumentPolicy(long maxSizeBytes, int maxPerApplication) {
    public DocumentPolicy {
        if (maxSizeBytes <= 0 || maxPerApplication <= 0) {
            throw new IllegalArgumentException("Document limits must be positive");
        }
    }

    /** Checks size and recognises the type; returns the detected type. */
    public DocumentType accept(byte[] content) {
        if (content.length == 0) {
            throw new IllegalArgumentException("Document is empty");
        }
        if (content.length > maxSizeBytes) {
            throw new DocumentRejectedException.TooLarge(maxSizeBytes);
        }
        return DocumentType.detect(content).orElseThrow(DocumentRejectedException.UnsupportedType::new);
    }

    public void requireRoomFor(int existing) {
        if (existing >= maxPerApplication) {
            throw new DocumentRejectedException.LimitReached(maxPerApplication);
        }
    }
}
