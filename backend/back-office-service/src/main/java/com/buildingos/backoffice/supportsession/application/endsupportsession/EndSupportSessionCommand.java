package com.buildingos.backoffice.supportsession.application.endsupportsession;

import java.util.UUID;

/** Ends one session with an optional audit reason. */
public record EndSupportSessionCommand(UUID sessionId, String reason) {}
