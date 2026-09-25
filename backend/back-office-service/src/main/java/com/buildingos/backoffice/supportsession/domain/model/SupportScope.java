package com.buildingos.backoffice.supportsession.domain.model;

import java.util.EnumSet;
import java.util.Set;

/** D-10 support session scopes ({@code SupportSession.permission_scope}); high-risk ones need elevated approval. */
public enum SupportScope {
    SUPPORT_VIEW_MEMBERS,
    SUPPORT_VIEW_UNITS,
    SUPPORT_VIEW_OWNERSHIP_HISTORY,
    SUPPORT_EDIT_UNIT,
    SUPPORT_MANAGE_MEMBERSHIP_INVITE,
    SUPPORT_REVOKE_MEMBERSHIP,
    SUPPORT_VIEW_SUBSCRIPTION,
    SUPPORT_VIEW_PAYMENTS,
    SUPPORT_REVERSE_PAYMENT,
    SUPPORT_TRANSFER_OWNERSHIP,
    SUPPORT_REMOVE_BUILDING_ADMIN,
    SUPPORT_EXPORT_FINANCIAL_UNRESTRICTED;

    /** §149.12 high-risk actions: blocked in an ordinary session until a SUPER_ADMIN approves (D-10, D-36d). */
    public static final Set<SupportScope> HIGH_RISK = EnumSet.of(SUPPORT_REVERSE_PAYMENT, SUPPORT_TRANSFER_OWNERSHIP,
            SUPPORT_REMOVE_BUILDING_ADMIN, SUPPORT_EXPORT_FINANCIAL_UNRESTRICTED);

    public boolean isHighRisk() { return HIGH_RISK.contains(this); }
}
