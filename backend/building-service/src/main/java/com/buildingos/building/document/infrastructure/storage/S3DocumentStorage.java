package com.buildingos.building.document.infrastructure.storage;

import com.buildingos.building.document.application.port.out.DocumentStorage;
import com.buildingos.building.shared.application.DependencyUnavailableException;
import java.io.InputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

/** S3 API adapter; works against MinIO (endpoint override, path-style) and AWS S3 unchanged. */
public class S3DocumentStorage implements DocumentStorage {
    private static final String DEPENDENCY = "Document storage";
    private final S3Client s3;
    private final String bucket;

    public S3DocumentStorage(S3Client s3, String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    @Override
    public void put(String key, byte[] content, String contentType) {
        try {
            s3.putObject(request -> request.bucket(bucket).key(key).contentType(contentType)
                    .contentLength((long) content.length), RequestBody.fromBytes(content));
        } catch (SdkException failed) {
            throw new DependencyUnavailableException(DEPENDENCY, failed);
        }
    }

    @Override
    public InputStream open(String key) {
        try {
            return s3.getObject(request -> request.bucket(bucket).key(key));
        } catch (SdkException failed) {
            throw new DependencyUnavailableException(DEPENDENCY, failed);
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3.deleteObject(request -> request.bucket(bucket).key(key));
        } catch (SdkException failed) {
            throw new DependencyUnavailableException(DEPENDENCY, failed);
        }
    }
}
