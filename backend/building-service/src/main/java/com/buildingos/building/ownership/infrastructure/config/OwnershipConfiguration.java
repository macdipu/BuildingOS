package com.buildingos.building.ownership.infrastructure.config;

import com.buildingos.building.building.domain.repository.BuildingMembershipRepository;
import com.buildingos.building.document.application.port.out.DocumentStorage;
import com.buildingos.building.document.domain.model.DocumentPolicy;
import com.buildingos.building.membership.application.BuildingAccess;
import com.buildingos.building.ownership.application.OwnershipWrites;
import com.buildingos.building.ownership.application.TransferDocuments;
import com.buildingos.building.ownership.application.downloadtransferdocument.DownloadTransferDocumentService;
import com.buildingos.building.ownership.application.downloadtransferdocument.DownloadTransferDocumentUseCase;
import com.buildingos.building.ownership.application.listtransferdocuments.ListTransferDocumentsService;
import com.buildingos.building.ownership.application.listtransferdocuments.ListTransferDocumentsUseCase;
import com.buildingos.building.ownership.application.removetransferdocument.RemoveTransferDocumentService;
import com.buildingos.building.ownership.application.removetransferdocument.RemoveTransferDocumentUseCase;
import com.buildingos.building.ownership.application.uploadtransferdocument.UploadTransferDocumentService;
import com.buildingos.building.ownership.application.uploadtransferdocument.UploadTransferDocumentUseCase;
import com.buildingos.building.ownership.application.assignownership.AssignOwnershipService;
import com.buildingos.building.ownership.application.assignownership.AssignOwnershipUseCase;
import com.buildingos.building.ownership.application.getcurrentownerships.GetCurrentOwnershipsService;
import com.buildingos.building.ownership.application.getcurrentownerships.GetCurrentOwnershipsUseCase;
import com.buildingos.building.ownership.application.getownershiphistory.GetOwnershipHistoryService;
import com.buildingos.building.ownership.application.getownershiphistory.GetOwnershipHistoryUseCase;
import com.buildingos.building.ownership.application.listmyproperties.ListMyPropertiesService;
import com.buildingos.building.ownership.application.listmyproperties.ListMyPropertiesUseCase;
import com.buildingos.building.ownership.application.transferownership.TransferOwnershipService;
import com.buildingos.building.ownership.application.transferownership.TransferOwnershipUseCase;
import com.buildingos.building.ownership.domain.repository.OwnershipRepository;
import com.buildingos.building.ownership.domain.repository.TransferDocumentRepository;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.shared.domain.repository.AuditRepository;
import com.buildingos.building.shared.domain.repository.OperationRepository;
import com.buildingos.building.shared.domain.repository.OutboxRepository;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OwnershipProperties.class)
public class OwnershipConfiguration {
    @Bean
    OwnershipWrites ownershipWrites(OwnershipRepository ownership, BuildingMembershipRepository memberships,
            OperationRepository operations, Clock clock, OwnershipProperties properties) {
        return new OwnershipWrites(ownership, memberships, operations, clock, properties.zone());
    }

    @Bean
    AssignOwnershipUseCase assignOwnership(BuildingAccess access, OwnershipWrites writes, AuditRepository audit,
            UnitOfWork uow, Clock clock) {
        return new AssignOwnershipService(access, writes, audit, uow, clock);
    }

    @Bean
    TransferOwnershipUseCase transferOwnership(BuildingAccess access, OwnershipWrites writes, AuditRepository audit,
            OutboxRepository outbox, UnitOfWork uow, Clock clock) {
        return new TransferOwnershipService(access, writes, audit, outbox, uow, clock);
    }

    @Bean
    GetCurrentOwnershipsUseCase getCurrentOwnerships(BuildingAccess access, OwnershipRepository ownership,
            UnitOfWork uow) {
        return new GetCurrentOwnershipsService(access, ownership, uow);
    }

    @Bean
    ListMyPropertiesUseCase listMyProperties(OwnershipRepository ownership, UnitOfWork uow) {
        return new ListMyPropertiesService(ownership, uow);
    }

    @Bean
    GetOwnershipHistoryUseCase getOwnershipHistory(BuildingAccess access, OwnershipRepository ownership,
            UnitOfWork uow) {
        return new GetOwnershipHistoryService(access, ownership, uow);
    }

    @Bean
    TransferDocuments transferDocuments(BuildingAccess access, OwnershipRepository ownership,
            DocumentStorage storage) {
        return new TransferDocuments(access, ownership, storage);
    }

    @Bean
    UploadTransferDocumentUseCase uploadTransferDocument(TransferDocuments transfers,
            TransferDocumentRepository documents, DocumentStorage storage, DocumentPolicy policy, AuditRepository audit,
            UnitOfWork uow, Clock clock, OwnershipProperties properties) {
        return new UploadTransferDocumentService(transfers, documents, storage, policy, audit, uow, clock,
                properties.maxDocumentsPerTransfer());
    }

    @Bean
    ListTransferDocumentsUseCase listTransferDocuments(TransferDocuments transfers,
            TransferDocumentRepository documents, UnitOfWork uow) {
        return new ListTransferDocumentsService(transfers, documents, uow);
    }

    @Bean
    DownloadTransferDocumentUseCase downloadTransferDocument(TransferDocuments transfers,
            TransferDocumentRepository documents, DocumentStorage storage, UnitOfWork uow) {
        return new DownloadTransferDocumentService(transfers, documents, storage, uow);
    }

    @Bean
    RemoveTransferDocumentUseCase removeTransferDocument(TransferDocuments transfers,
            TransferDocumentRepository documents, AuditRepository audit, UnitOfWork uow, Clock clock) {
        return new RemoveTransferDocumentService(transfers, documents, audit, uow, clock);
    }
}
