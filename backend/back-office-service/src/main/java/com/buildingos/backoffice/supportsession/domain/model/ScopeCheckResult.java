package com.buildingos.backoffice.supportsession.domain.model;

/** Whether a support session may currently use a scope (D-36). */
public enum ScopeCheckResult { ALLOWED, PENDING_APPROVAL, NOT_GRANTED, SESSION_ENDED }
