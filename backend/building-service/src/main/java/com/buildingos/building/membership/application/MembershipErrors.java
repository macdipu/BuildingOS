package com.buildingos.building.membership.application;

import com.buildingos.building.shared.application.BusinessRuleException;

public final class MembershipErrors {
    private MembershipErrors() {}

    public static BusinessRuleException invitationNotFound() {
        return BusinessRuleException.notFound("INVITATION_NOT_FOUND", "Invitation does not exist");
    }

    public static BusinessRuleException membershipNotFound() {
        return BusinessRuleException.notFound("MEMBERSHIP_NOT_FOUND", "Membership does not exist");
    }

    public static BusinessRuleException buildingReadOnly() {
        return BusinessRuleException.conflict("BUILDING_READ_ONLY", "Suspended buildings are read-only");
    }

    public static BusinessRuleException idempotencyConflict() {
        return BusinessRuleException.conflict("IDEMPOTENCY_CONFLICT",
                "operationId was already used for a different request");
    }
}
