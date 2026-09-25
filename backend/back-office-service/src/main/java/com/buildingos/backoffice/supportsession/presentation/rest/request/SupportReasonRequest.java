package com.buildingos.backoffice.supportsession.presentation.rest.request;

/** Body carrying an audit reason: optional for end and approve, required for deny. */
public record SupportReasonRequest(String reason) {}
