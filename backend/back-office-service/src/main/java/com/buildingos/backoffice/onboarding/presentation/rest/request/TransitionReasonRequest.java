package com.buildingos.backoffice.onboarding.presentation.rest.request;

/** Optional body of a session transition: an audit reason recorded on the transition row. */
public record TransitionReasonRequest(String reason) {}
