package com.buildingos.building.document.domain.model;

import java.util.Arrays;
import java.util.Optional;

/** Accepted verification file types, recognised by their leading bytes rather than a client-declared header. */
public enum DocumentType {
    PDF("application/pdf", new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D}),
    JPEG("image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
    PNG("image/png", new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});

    private final String contentType;
    private final byte[] signature;

    DocumentType(String contentType, byte[] signature) {
        this.contentType = contentType;
        this.signature = signature;
    }

    public String contentType() { return contentType; }

    public static Optional<DocumentType> detect(byte[] content) {
        return Arrays.stream(values()).filter(type -> type.matches(content)).findFirst();
    }

    public static DocumentType fromContentType(String contentType) {
        return Arrays.stream(values()).filter(type -> type.contentType.equals(contentType)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown document content type " + contentType));
    }

    private boolean matches(byte[] content) {
        return content.length >= signature.length
                && Arrays.equals(content, 0, signature.length, signature, 0, signature.length);
    }
}
