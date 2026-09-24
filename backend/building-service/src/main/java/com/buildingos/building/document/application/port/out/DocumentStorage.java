package com.buildingos.building.document.application.port.out;

import java.io.InputStream;

/** Object storage for document bytes (D-28): MinIO locally, S3 in production. Failures surface as unavailable. */
public interface DocumentStorage {
    void put(String key, byte[] content, String contentType);
    InputStream open(String key);
    void delete(String key);
}
