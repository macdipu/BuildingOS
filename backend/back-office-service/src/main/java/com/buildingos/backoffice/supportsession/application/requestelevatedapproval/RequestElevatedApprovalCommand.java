package com.buildingos.backoffice.supportsession.application.requestelevatedapproval;

import com.buildingos.backoffice.supportsession.domain.model.SupportScope;
import java.util.UUID;

public record RequestElevatedApprovalCommand(UUID sessionId, SupportScope scope, String reason) {}
