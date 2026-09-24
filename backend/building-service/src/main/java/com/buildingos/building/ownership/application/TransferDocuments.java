package com.buildingos.building.ownership.application;

import com.buildingos.building.building.domain.model.BuildingRole;
import com.buildingos.building.document.application.port.out.DocumentStorage;
import com.buildingos.building.membership.application.BuildingAccess;
import com.buildingos.building.membership.application.BuildingGrant;
import com.buildingos.building.ownership.domain.model.OwnershipTransfer;
import com.buildingos.building.ownership.domain.model.TransferDocument;
import com.buildingos.building.ownership.domain.repository.OwnershipRepository;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.BusinessRuleException;
import java.util.UUID;

/**
 * Transfer-document authorization (TECH-SPEC-F4 Security): building/platform admins, or the transfer's source or
 * recipient while they hold a current OWNER membership. Anyone else sees the transfer as missing. Writes are
 * admin-only, like every other ownership write. Callers run inside one transaction.
 */
public final class TransferDocuments {
    private final BuildingAccess access;
    private final OwnershipRepository ownership;
    private final DocumentStorage storage;

    public TransferDocuments(BuildingAccess access, OwnershipRepository ownership, DocumentStorage storage) {
        this.access = access;
        this.ownership = ownership;
        this.storage = storage;
    }

    public record Target(BuildingGrant grant, OwnershipTransfer transfer) {
    }

    public OwnershipTransfer requireReadable(Actor actor, UUID buildingId, UUID unitId, UUID transferId) {
        var grant = access.requireMemberToRead(actor, buildingId);
        var transfer = find(buildingId, unitId, transferId);
        boolean party = grant.roles().contains(BuildingRole.OWNER) && TransferDocument.isParty(transfer,
                actor.userId());
        if (!grant.isAdmin() && !party) {
            throw transferNotFound();
        }
        return transfer;
    }

    public Target requireWritable(Actor actor, UUID buildingId, UUID unitId, UUID transferId) {
        var grant = access.requireAdminToWrite(actor, buildingId);
        return new Target(grant, find(buildingId, unitId, transferId));
    }

    /** Leaves an orphan object behind on failure rather than failing a request whose data change stands. */
    public void deleteQuietly(String objectKey) {
        try {
            storage.delete(objectKey);
        } catch (RuntimeException ignored) {
            // A missing or unreachable object is harmless here; no row points at it.
        }
    }

    public static BusinessRuleException documentNotFound() {
        return BusinessRuleException.notFound("DOCUMENT_NOT_FOUND", "Document does not exist for this transfer");
    }

    private OwnershipTransfer find(UUID buildingId, UUID unitId, UUID transferId) {
        return ownership.findTransfer(buildingId, unitId, transferId).orElseThrow(TransferDocuments::transferNotFound);
    }

    private static BusinessRuleException transferNotFound() {
        return BusinessRuleException.notFound("TRANSFER_NOT_FOUND", "Ownership transfer does not exist for this unit");
    }
}
