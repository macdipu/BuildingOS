package com.buildingos.building.document.infrastructure.config;

import com.buildingos.building.buildingapplication.application.ApplicationChanges;
import com.buildingos.building.buildingapplication.domain.repository.BuildingApplicationRepository;
import com.buildingos.building.document.application.downloaddocument.DownloadDocumentService;
import com.buildingos.building.document.application.downloaddocument.DownloadDocumentUseCase;
import com.buildingos.building.document.application.listdocuments.ListDocumentsService;
import com.buildingos.building.document.application.listdocuments.ListDocumentsUseCase;
import com.buildingos.building.document.application.port.out.DocumentStorage;
import com.buildingos.building.document.application.removedocument.RemoveDocumentService;
import com.buildingos.building.document.application.removedocument.RemoveDocumentUseCase;
import com.buildingos.building.document.application.uploaddocument.UploadDocumentService;
import com.buildingos.building.document.application.uploaddocument.UploadDocumentUseCase;
import com.buildingos.building.document.domain.model.DocumentPolicy;
import com.buildingos.building.document.domain.repository.ApplicationDocumentRepository;
import com.buildingos.building.document.infrastructure.storage.S3DocumentStorage;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DocumentProperties.class)
public class DocumentConfiguration {
    @Bean(destroyMethod = "close")
    S3Client documentS3Client(DocumentProperties properties) {
        var s3 = properties.s3();
        var builder = S3Client.builder()
                .region(Region.of(s3.region()))
                .forcePathStyle(s3.pathStyle())
                .httpClient(UrlConnectionHttpClient.builder()
                        .connectionTimeout(Duration.ofSeconds(3)).socketTimeout(Duration.ofSeconds(30)).build());
        if (s3.endpoint() != null && !s3.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(s3.endpoint()));
        }
        if (s3.accessKey() != null && !s3.accessKey().isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(s3.accessKey(), s3.secretKey())));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.builder().build());
        }
        return builder.build();
    }

    @Bean
    DocumentStorage documentStorage(S3Client documentS3Client, DocumentProperties properties) {
        return new S3DocumentStorage(documentS3Client, properties.s3().bucket());
    }

    @Bean
    DocumentPolicy documentPolicy(DocumentProperties properties) {
        return new DocumentPolicy(properties.maxSizeBytes(), properties.maxPerApplication());
    }

    @Bean
    UploadDocumentUseCase uploadDocument(BuildingApplicationRepository applications,
            ApplicationDocumentRepository documents, DocumentStorage storage, DocumentPolicy policy, UnitOfWork uow,
            Clock clock) {
        return new UploadDocumentService(applications, documents, storage, policy, uow, clock);
    }

    @Bean
    RemoveDocumentUseCase removeDocument(BuildingApplicationRepository applications,
            ApplicationDocumentRepository documents, DocumentStorage storage, UnitOfWork uow) {
        return new RemoveDocumentService(applications, documents, storage, uow);
    }

    @Bean
    DownloadDocumentUseCase downloadDocument(ApplicationChanges applications, ApplicationDocumentRepository documents,
            DocumentStorage storage) {
        return new DownloadDocumentService(applications, documents, storage);
    }

    @Bean
    ListDocumentsUseCase listDocuments(ApplicationChanges applications, ApplicationDocumentRepository documents) {
        return new ListDocumentsService(applications, documents);
    }
}
