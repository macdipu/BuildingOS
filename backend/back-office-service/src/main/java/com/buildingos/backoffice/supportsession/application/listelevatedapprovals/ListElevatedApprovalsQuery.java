package com.buildingos.backoffice.supportsession.application.listelevatedapprovals;

import com.buildingos.backoffice.supportsession.domain.model.ElevatedApprovalStatus;

/** Optional status filter; null matches every status. */
public record ListElevatedApprovalsQuery(ElevatedApprovalStatus status, int page, int size) {}
